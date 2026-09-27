-- =========================================================================
-- AI Video Studio - reference schema dump
--
-- This file is a convenience copy for quickly reading the schema. It is NOT
-- run automatically and is not the source of truth - Flyway migrations under
-- backend/src/main/resources/db/migration/ are what actually create/version
-- the schema when the backend starts. If you ever change the schema, edit a
-- new Flyway migration (never this file) and re-generate this copy.
-- =========================================================================

-- =========================================================================
-- AI Video Studio - Initial schema
-- MySQL 8.x
-- =========================================================================

CREATE TABLE users (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(150)    NOT NULL,
    email             VARCHAR(190)    NOT NULL,
    mobile            VARCHAR(20)     NOT NULL,
    password_hash     VARCHAR(255)    NOT NULL,
    email_verified    BOOLEAN         NOT NULL DEFAULT FALSE,
    status            VARCHAR(30)     NOT NULL DEFAULT 'PENDING_VERIFICATION',
    role              VARCHAR(20)     NOT NULL DEFAULT 'USER',
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE','INACTIVE','PENDING_VERIFICATION','SUSPENDED')),
    CONSTRAINT ck_users_role CHECK (role IN ('USER','ADMIN'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_status ON users (status);


CREATE TABLE otp_verifications (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT          NOT NULL,
    purpose       VARCHAR(30)     NOT NULL,
    otp_hash      VARCHAR(255)    NOT NULL,
    expires_at    TIMESTAMP       NOT NULL,
    attempts      INT             NOT NULL DEFAULT 0,
    consumed      BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_otp_purpose CHECK (purpose IN ('REGISTRATION','PASSWORD_RESET'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_otp_user_purpose ON otp_verifications (user_id, purpose, consumed);


CREATE TABLE refresh_tokens (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT          NOT NULL,
    token_hash    VARCHAR(255)    NOT NULL,
    expires_at    TIMESTAMP       NOT NULL,
    revoked       BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_refresh_token_hash UNIQUE (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_refresh_user ON refresh_tokens (user_id);


CREATE TABLE wallets (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT          NOT NULL,
    diamond_balance   BIGINT          NOT NULL DEFAULT 0,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_wallet_user UNIQUE (user_id),
    CONSTRAINT ck_wallet_balance_non_negative CHECK (diamond_balance >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_wallets_user_id ON wallets (user_id);


CREATE TABLE wallet_transactions (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id           BIGINT          NOT NULL,
    wallet_id         BIGINT          NOT NULL,
    type              VARCHAR(30)     NOT NULL,
    amount            BIGINT          NOT NULL,
    balance_before    BIGINT          NOT NULL,
    balance_after     BIGINT          NOT NULL,
    reference_id      VARCHAR(100)    NULL,
    description       VARCHAR(255)    NULL,
    created_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wtxn_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_wtxn_wallet FOREIGN KEY (wallet_id) REFERENCES wallets (id) ON DELETE CASCADE,
    CONSTRAINT ck_wtxn_type CHECK (type IN ('PURCHASE','VIDEO_GENERATION','DAILY_REWARD','AD_REWARD','REFUND','ADMIN_ADJUSTMENT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_wtxn_user_id ON wallet_transactions (user_id);
CREATE INDEX idx_wtxn_created_at ON wallet_transactions (created_at);
CREATE INDEX idx_wtxn_reference ON wallet_transactions (reference_id);


CREATE TABLE diamond_packages (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    name          VARCHAR(100)    NOT NULL,
    diamonds      INT             NOT NULL,
    price         DECIMAL(10,2)   NOT NULL,
    currency      VARCHAR(10)     NOT NULL DEFAULT 'INR',
    active        BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT ck_package_diamonds_positive CHECK (diamonds > 0),
    CONSTRAINT ck_package_price_positive CHECK (price > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_packages_active ON diamond_packages (active);


CREATE TABLE payments (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    package_id      BIGINT          NOT NULL,
    order_id        VARCHAR(100)    NOT NULL,
    payment_id      VARCHAR(100)    NULL,
    amount          DECIMAL(10,2)   NOT NULL,
    currency        VARCHAR(10)     NOT NULL DEFAULT 'INR',
    gateway         VARCHAR(30)     NOT NULL DEFAULT 'MOCK',
    status          VARCHAR(20)     NOT NULL DEFAULT 'CREATED',
    diamonds        INT             NOT NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMP       NULL,
    CONSTRAINT fk_payment_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_payment_package FOREIGN KEY (package_id) REFERENCES diamond_packages (id),
    CONSTRAINT uq_payment_order_id UNIQUE (order_id),
    CONSTRAINT ck_payment_status CHECK (status IN ('CREATED','PENDING','SUCCESS','FAILED','REFUNDED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_payments_user_id ON payments (user_id);
CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_status ON payments (status);


CREATE TABLE video_jobs (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    prompt              TEXT            NOT NULL,
    duration            INT             NOT NULL,
    diamond_cost        INT             NOT NULL,
    provider_job_id     VARCHAR(150)    NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'QUEUED',
    video_url           VARCHAR(500)    NULL,
    thumbnail_url       VARCHAR(500)    NULL,
    storage_key         VARCHAR(255)    NULL,
    error_message       VARCHAR(500)    NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at          TIMESTAMP       NULL,
    completed_at        TIMESTAMP       NULL,
    CONSTRAINT fk_video_job_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_video_status CHECK (status IN ('QUEUED','PROCESSING','COMPLETED','FAILED','CANCELLED')),
    CONSTRAINT ck_video_duration CHECK (duration IN (4, 8))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_video_jobs_user_id ON video_jobs (user_id);
CREATE INDEX idx_video_jobs_status ON video_jobs (status);
CREATE INDEX idx_video_jobs_created_at ON video_jobs (created_at);


CREATE TABLE moderation_results (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    input_type      VARCHAR(20)     NOT NULL DEFAULT 'PROMPT',
    input_snippet   VARCHAR(500)    NULL,
    decision        VARCHAR(20)     NOT NULL,
    reason          VARCHAR(255)    NULL,
    reference_id    VARCHAR(100)    NULL,
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_moderation_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_moderation_decision CHECK (decision IN ('ALLOWED','BLOCKED')),
    CONSTRAINT ck_moderation_input_type CHECK (input_type IN ('PROMPT','IMAGE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_moderation_user_id ON moderation_results (user_id);
CREATE INDEX idx_moderation_decision ON moderation_results (decision);
CREATE INDEX idx_moderation_created_at ON moderation_results (created_at);


CREATE TABLE ad_rewards (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    provider            VARCHAR(50)     NOT NULL,
    verification_id     VARCHAR(150)    NOT NULL,
    reward_amount       INT             NOT NULL,
    status              VARCHAR(20)     NOT NULL,
    reward_date         DATE            NOT NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ad_reward_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_ad_reward_verification UNIQUE (provider, verification_id),
    CONSTRAINT ck_ad_reward_status CHECK (status IN ('PENDING','VERIFIED','CREDITED','FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ad_rewards_user_id ON ad_rewards (user_id);
CREATE INDEX idx_ad_rewards_reward_date ON ad_rewards (reward_date);


CREATE TABLE daily_rewards (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT          NOT NULL,
    reward_date     DATE            NOT NULL,
    diamonds        INT             NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'CREDITED',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_daily_reward_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uq_daily_reward_user_date UNIQUE (user_id, reward_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_daily_rewards_user_id ON daily_rewards (user_id);
