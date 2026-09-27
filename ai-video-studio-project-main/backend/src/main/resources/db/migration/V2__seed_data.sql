-- =========================================================================
-- AI Video Studio - Development seed data
--
-- ⚠️  DEVELOPMENT ONLY. These accounts and their passwords are publicly
--     documented in this repository's README. Delete or change them before
--     any staging/production deployment.
--
--     admin@example.com / Admin@12345   (ADMIN, active)
--     user@example.com  / Test@12345    (USER, active, seeded with 100 diamonds)
-- =========================================================================

INSERT INTO users (name, email, mobile, password_hash, email_verified, status, role)
VALUES ('Platform Admin', 'admin@example.com', '9999999999',
        '$2b$12$7ZlgxuIEkTwwsZQdfnxhAOz4A4PjZC7v9ZUC4Rj/pOiO/nkWmpevu',
        TRUE, 'ACTIVE', 'ADMIN');

INSERT INTO users (name, email, mobile, password_hash, email_verified, status, role)
VALUES ('Test User', 'user@example.com', '9888888888',
        '$2b$12$/9lMVcckXzufaDwwyt321.jShPygmqHZ1lWUBUgMWeKUiLzdqpY5q',
        TRUE, 'ACTIVE', 'USER');

INSERT INTO wallets (user_id, diamond_balance)
SELECT id, 0 FROM users WHERE email = 'admin@example.com';

INSERT INTO wallets (user_id, diamond_balance)
SELECT id, 100 FROM users WHERE email = 'user@example.com';

INSERT INTO wallet_transactions (user_id, wallet_id, type, amount, balance_before, balance_after, reference_id, description)
SELECT u.id, w.id, 'ADMIN_ADJUSTMENT', 100, 0, 100, 'seed-data', 'Development seed balance'
FROM users u JOIN wallets w ON w.user_id = u.id
WHERE u.email = 'user@example.com';

INSERT INTO diamond_packages (name, diamonds, price, currency, active)
VALUES ('Starter Pack', 50, 200.00, 'INR', TRUE),
       ('Value Pack', 100, 300.00, 'INR', TRUE);
