# AI Video Studio

A full-stack SaaS platform where authenticated users spend virtual **diamonds** to generate short AI videos from text prompts. Includes auth with OTP verification, a concurrency-safe diamond wallet, diamond packages purchased through a payment-provider abstraction, server-side content moderation, asynchronous video generation, a rewarded-ad daily bonus, and a full admin console.

Every external integration (email, payments, AI video generation, content moderation, rewarded ads, object storage) is implemented as an interface with a working **mock** implementation, so the entire application runs locally with **zero external accounts or API keys**.

---

## Table of contents

1. [Features](#features)
2. [Architecture](#architecture)
3. [Technology stack](#technology-stack)
4. [Project structure](#project-structure)
5. [Requirements](#requirements)
6. [Quick start (Docker)](#quick-start-docker)
7. [Manual setup](#manual-setup)
8. [Mock mode](#mock-mode)
9. [Seed accounts](#seed-accounts)
10. [Environment variables](#environment-variables)
11. [API overview & Swagger](#api-overview--swagger)
12. [Testing](#testing)
13. [Security notes](#security-notes)
14. [Going to production](#going-to-production)
15. [Wiring up real providers](#wiring-up-real-providers)
16. [Known simplifications](#known-simplifications)

---

## Features

- **Auth**: registration with 6-digit OTP email verification, JWT access + rotating refresh tokens, forgot/reset/change password, role-based access (`USER` / `ADMIN`).
- **Wallet**: every user has a diamond wallet; every balance change is recorded as a `wallet_transaction`. Debits use **pessimistic row locking** so concurrent spends can never overdraw a balance or go negative.
- **Video generation**: server-computed pricing (4s = 5💎, 8s = 10💎), pre-generation content moderation, asynchronous processing (the HTTP request returns immediately with `QUEUED`; status is polled from the frontend), automatic diamond refund on failure.
- **Diamond packages & payments**: packages are served from the database (never hard-coded in the frontend); payments go through a provider abstraction with **server-side verification** and an **idempotent webhook** - the frontend claiming "payment succeeded" is never trusted on its own.
- **Content moderation**: every prompt is checked before generation; blocked content never reaches the video provider and returns a generic, non-revealing message to the user.
- **Daily rewarded-ad bonus**: 3 diamonds/day, gated by server-side ad verification and a database-enforced one-per-day limit.
- **Admin console**: dashboard metrics, user management (activate/deactivate/suspend), payments, videos, moderation log, transactions, and revenue - all behind `ROLE_ADMIN`, enforced server-side.

## Architecture

```
                         USER
                          │
                          ▼
                  React + Vite (SPA)
                          │  HTTPS (JWT bearer)
                          ▼
                 Spring Boot REST API
     ┌───────────┬─────────────┬─────────────┬────────────┐
     ▼           ▼             ▼             ▼            ▼
  Auth/JWT    Wallet      Moderation      Video Jobs    Admin
     │           │             │             │
     │           ▼             ▼             ▼
     │      Payments →   (blocks before  AI Video Provider
     │      Provider      diamonds are      (mock/real)
     │                    touched)              │
     │                                           ▼
     │                                    Object Storage
     ▼                                     (mock/real)
   MySQL  ◄───────────────────────────────────────┘
 (also stores video_url / thumbnail_url / storage_key,
  never the video binary itself)
```

Video generation is deliberately asynchronous: `POST /api/videos/generate` validates, moderates, prices, and debits diamonds inside **one transaction**, then returns `202 Accepted` with `status: "QUEUED"` immediately. Only *after* that transaction commits does a background task (Spring `@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT)`) call the video provider and progress the job through `PROCESSING` → `COMPLETED`/`FAILED` - so the wallet's row lock is never held while waiting on a slow external call, and the design swaps cleanly onto a real queue (RabbitMQ/Kafka/SQS) later without touching the business logic in `VideoService`.

## Technology stack

**Backend**: Java 21 · Spring Boot 4 (Web MVC, Security, Data JPA, Validation, Mail, Actuator) · MySQL 8 · Flyway · JJWT · BCrypt · springdoc-openapi (Swagger) · Maven · JUnit 5 / Mockito / H2

**Frontend**: React 19 · Vite · React Router · Axios · Context API · Recharts (admin charts) · lucide-react (icons) · plain CSS design system

**Infra**: Docker, Docker Compose, Nginx (serves the built frontend + SPA fallback)

## Project structure

```
ai-video-studio/
├── backend/                   Spring Boot API (Maven project)
│   └── src/main/java/com/aivideo/platform/
│       ├── config/            Security, CORS, OpenAPI, Jackson, Async config
│       ├── security/          JWT service/filter, rate limiter, UserDetails
│       ├── auth/               Registration, OTP, login, password reset
│       ├── user/               Profile
│       ├── wallet/             Diamond balance + transactions (concurrency-safe)
│       ├── diamond/            Purchasable diamond packages
│       ├── payment/            Orders, verification, webhook, provider abstraction
│       ├── video/              Video jobs, async pipeline, provider abstraction
│       ├── moderation/         Prompt/image moderation, provider abstraction
│       ├── ads/                 Daily reward + ad-verification provider abstraction
│       ├── storage/             Object storage abstraction
│       ├── admin/               Cross-cutting dashboard/revenue aggregation
│       └── common/              ApiResponse/ErrorResponse envelopes, exceptions
├── frontend/                   React + Vite SPA
│   └── src/{pages,components,services,context,utils,styles}/
├── database/                   Reference schema/seed SQL (Flyway is the source of truth)
├── docker/                     (reserved - both apps ship their own Dockerfile)
├── docker-compose.yml
├── .env.example
└── README.md                   (this file)
```

## Requirements

- Java 21 (JDK)
- Maven (or use the included `./mvnw` if you add the wrapper jar - see note below)
- Node.js 20+ and npm
- MySQL 8.x (or Docker, to run it in a container)
- Docker + Docker Compose (optional, for the one-command path)

> **Maven wrapper note**: this project's `pom.xml` is wrapper-ready, but the `mvnw`/`mvnw.cmd` scripts and `.mvn/wrapper/maven-wrapper.properties` binary bootstrap aren't included in this generated output. Run `mvn -N io.takari:maven:wrapper` once inside `backend/` (or `mvn wrapper:wrapper`) to generate them if you'd rather not rely on a system-installed Maven, or just use your system `mvn`.

## Quick start (Docker)

```bash
cp .env.example .env
# Edit .env: at minimum set JWT_SECRET to a long random string, e.g.
#   openssl rand -base64 48

docker compose up --build
```

- Frontend: http://localhost:5173
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- MySQL: localhost:3306 (`aivideo_studio` / root / value of `DB_PASSWORD`)

Flyway runs automatically on backend startup and creates the schema + seed data (see [Seed accounts](#seed-accounts)).

## Manual setup

### 1. MySQL

```sql
CREATE DATABASE aivideo_studio CHARACTER SET utf8mb4;
```

### 2. Backend

```bash
cd backend
cp ../.env.example .env   # or export the variables another way; Spring Boot reads
                           # them via ${VAR_NAME} placeholders in application.properties -
                           # plain `cp` doesn't inject them, use direnv/export/your IDE's
                           # env file support, or pass -D flags, e.g.:
export DB_USERNAME=root DB_PASSWORD=root JWT_SECRET=$(openssl rand -base64 48)

# macOS/Linux
mvn spring-boot:run

# Windows
mvn.cmd spring-boot:run
```

The API starts on `http://localhost:8080`.

### 3. Frontend

```bash
cd frontend
cp .env.example .env   # adjust VITE_API_BASE_URL if needed
npm install
npm run dev
```

The app starts on `http://localhost:5173`.

## Mock mode

With `APP_MODE=mock` (the default), these six providers back every external integration - no credentials needed:

| Interface | Mock implementation | Behavior |
|---|---|---|
| `EmailService` | `MockEmailService` | Logs the OTP to the backend console instead of sending email |
| `PaymentProvider` | `MockPaymentProvider` | Simulates order creation & verification; **really** computes/checks HMAC-SHA256 webhook signatures |
| `VideoProvider` | `MockVideoProvider` | Simulates `QUEUED → PROCESSING → COMPLETED` with realistic delay |
| `ModerationProvider` | `MockModerationProvider` | Keyword-based blocklist (see its class comment - replace before production) |
| `AdProvider` | `MockAdProvider` | Accepts any verification id as proof of a watched ad |
| `StorageService` | `MockStorageService` | Returns deterministic fake CDN URLs; nothing is actually uploaded |

Two useful test hooks in mock mode:
- Include the text `FAIL_TEST` in a video prompt to force that generation to fail (and verify the diamond refund).
- Pass `providerPaymentId: "FAIL_TEST"` to `POST /api/payments/verify` to force a payment failure.

## Seed accounts

> ⚠️ **Development only** - change or delete these before any staging/production deployment.

| Role | Email | Password |
|---|---|---|
| Admin | `admin@example.com` | `Admin@12345` |
| User (seeded with 100 💎) | `user@example.com` | `Test@12345` |

Diamond packages seeded: 50💎 for ₹200, 100💎 for ₹300.

## Environment variables

See [`.env.example`](./.env.example) for the full, documented list. Summary:

| Variable | Required in mock mode? | Purpose |
|---|---|---|
| `JWT_SECRET` | **Yes** | Signs access tokens. Use `openssl rand -base64 48`. |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | Yes | MySQL connection |
| `APP_MODE` | No (defaults to `mock`) | `mock` or `live` |
| `FRONTEND_URL` | No | CORS allow-list origin |
| `MAIL_*` | Only if `APP_MODE=live` | SMTP for real emails |
| `AI_VIDEO_API_KEY` | Only if `APP_MODE=live` | Real video generation provider |
| `PAYMENT_KEY` / `PAYMENT_SECRET` | Only if `APP_MODE=live` | Real payment gateway |
| `STORAGE_BUCKET` / `STORAGE_ACCESS_KEY` / `STORAGE_SECRET_KEY` | Only if `APP_MODE=live` | Real object storage (e.g. S3) |
| `AD_PROVIDER_KEY` | Only if `APP_MODE=live` | Real rewarded-ad network |

Frontend secrets: **none** of the keys above are ever sent to the browser. The only frontend env var is `VITE_API_BASE_URL` (the public API URL).

## API overview & Swagger

Full interactive documentation (with a JWT "Authorize" button): **http://localhost:8080/swagger-ui.html**

<details>
<summary>Endpoint list</summary>

```
AUTH
POST   /api/auth/register
POST   /api/auth/verify-otp
POST   /api/auth/resend-otp
POST   /api/auth/login
POST   /api/auth/refresh
POST   /api/auth/forgot-password
POST   /api/auth/reset-password
POST   /api/auth/change-password        (requires auth)

USER
GET    /api/users/me
PUT    /api/users/me

WALLET
GET    /api/wallet
GET    /api/wallet/transactions

DIAMONDS
GET    /api/diamonds/packages           (public)

PAYMENTS
POST   /api/payments/create-order
POST   /api/payments/verify
POST   /api/payments/webhook            (public - signature-authenticated, not JWT)
GET    /api/payments/history

VIDEOS
POST   /api/videos/generate
GET    /api/videos
GET    /api/videos/{id}
DELETE /api/videos/{id}

REWARDS
GET    /api/rewards/status
POST   /api/rewards/ad/verify

ADMIN (all require ROLE_ADMIN)
GET    /api/admin/dashboard
GET    /api/admin/users
PUT    /api/admin/users/{id}/status
GET    /api/admin/payments
GET    /api/admin/videos
GET    /api/admin/moderation
GET    /api/admin/transactions
GET    /api/admin/revenue
```
</details>

<details>
<summary>Example: register → verify → login</summary>

```bash
curl -X POST localhost:8080/api/auth/register -H "Content-Type: application/json" -d '{
  "name": "John", "email": "john@example.com", "mobile": "9999999999",
  "password": "Password@123", "confirmPassword": "Password@123"
}'
# => { "success": true, "message": "OTP sent", "data": null }
# Watch the backend console for: [DEV-ONLY MOCK EMAIL] ... OTP: 123456

curl -X POST localhost:8080/api/auth/verify-otp -H "Content-Type: application/json" -d '{
  "email": "john@example.com", "otp": "123456"
}'
# => { "success": true, "data": { "accessToken": "...", "refreshToken": "...", "user": {...} } }
```
</details>

All responses follow the same envelope:

```jsonc
// success
{ "success": true, "message": "...", "data": { /* or null */ } }
// error
{ "success": false, "message": "...", "errorCode": "INSUFFICIENT_DIAMONDS", "timestamp": "..." }
```

## Testing

```bash
cd backend
mvn test
```

Tests run against an in-memory H2 database (no MySQL needed) and cover, among others:

- **`WalletConcurrencyTest`** - the exact scenario called out in the spec: a wallet with 5 diamonds receives two simultaneous 5-diamond debit requests; exactly one succeeds, the balance never goes negative.
- `OtpServiceTest` - expiry, single-use, max attempts, resend cooldown.
- `AuthFlowIntegrationTest` - full HTTP-level register → verify → login journey via MockMvc.
- `PaymentServiceIdempotencyTest` - verifying (or webhook-delivering) the same payment twice only credits diamonds once; bad webhook signatures are rejected.
- `VideoServiceTest` - server-side pricing, insufficient-balance rejection, async completion, and refund-on-failure.
- `ModerationServiceTest` - blocked prompts never reach the wallet or video provider.
- `RewardServiceTest` - a second daily-reward claim on the same day is rejected and does not double-credit.

## Security notes

- Passwords: BCrypt (cost 12). OTPs and refresh tokens: SHA-256 hashed at rest, never stored in plaintext.
- All diamond/price/role/payment-success/reward-eligibility decisions are made **server-side only** - the backend never trusts these values from the client (see `VideoService.priceFor`, `PaymentService.verify`/`handleWebhook`, `SecurityConfig`'s role-based rules).
- JWT is stateless; refresh tokens are opaque, hashed, single-use (rotated on every refresh), and individually revocable.
- Rate limiting on auth/reward/generate endpoints via `RateLimitingFilter` - **this is an in-memory, single-instance limiter**; see the class-level comment for how to swap in Bucket4j + Redis for a multi-instance deployment.
- CORS is locked to one configured origin (`FRONTEND_URL`), never `*` with credentials.
- Stack traces and internal error details are never returned to the client (`GlobalExceptionHandler`).

## Going to production

1. Set `APP_MODE=live` and wire up real implementations for whichever providers you need (see below) - don't leave `Mock*` beans active.
2. Generate a strong `JWT_SECRET` via your platform's secret manager, not a `.env` file in source control.
3. Terminate TLS in front of the API (load balancer / reverse proxy / cloud provider) - Spring Boot itself isn't configured for TLS here.
4. Replace `RateLimitingFilter` with a distributed limiter if running more than one backend instance.
5. Point `FRONTEND_URL` (CORS) and `VITE_API_BASE_URL` at your real deployed origins.
6. Set `SWAGGER_ENABLED=false` (or leave the prod profile default) if you don't want Swagger UI public.
7. Review `application-prod.properties` and the seed migration - **delete or rotate the seeded admin/test accounts**.

## Wiring up real providers

Every provider is a small interface with exactly one mock implementation. To go live, add a new `@Component` implementing the same interface, guard it with `@ConditionalOnProperty(name = "app.mode", havingValue = "live")`, and it will automatically replace the mock (Spring picks whichever bean matches the active `app.mode`).

| Interface | Where | What a real implementation needs |
|---|---|---|
| `EmailService` | `auth/` | Already has a working SMTP implementation - `SmtpEmailService`. Just set `MAIL_*` and `APP_MODE=live`. |
| `PaymentProvider` | `payment/provider/` | Integrate your chosen gateway's actual SDK/API (Razorpay, Stripe, etc.) for `createOrder`/`verifyPayment`/`verifyWebhookSignature`. |
| `VideoProvider` | `video/provider/` | Call your AI video generation API's `create`/`status`/`cancel` endpoints with `AI_VIDEO_API_KEY`. |
| `ModerationProvider` | `moderation/provider/` | Call a real moderation API or classifier - the mock's keyword list is not production-grade. |
| `AdProvider` | `ads/provider/` | Verify the rewarded-ad network's server-to-server callback/signature. |
| `StorageService` | `storage/` | Upload to S3 (or similar) with the AWS SDK using `STORAGE_*`, return a (ideally CDN-fronted) URL. |

## Known simplifications

Being upfront about where this generated baseline trades completeness for a working, comprehensible whole:

- The rate limiter and async video pipeline are in-memory/single-instance by design for local development; the README and code comments above each note exactly what to swap in for a multi-instance production deployment.
- `MockModerationProvider`'s blocklist is intentionally small and illustrative, not production-grade - see its class comment.
- Image-to-video / reference-image upload isn't wired into the UI (the spec only requires moderating it "if implemented"); `ModerationProvider.checkImage` exists and is ready for that when you add it.
- Admin analytics are a focused, useful set of metrics and one chart rather than an exhaustive BI dashboard.
- This backend could not be compiled in the sandbox that generated it (no Maven Central access there) - it was written carefully by hand and follows conservative, well-established Spring Boot patterns throughout, but **run `mvn test` / `mvn spring-boot:run` yourself as the first step**. The frontend *was* built successfully with `npm install && npm run build` during generation.
#   a i - v i d e o - s t u d i o - p r o j e c t  
 