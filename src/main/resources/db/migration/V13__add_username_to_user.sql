-- V13__add_username_to_user.sql

-- 1. Add username column as nullable first
ALTER TABLE users ADD COLUMN username VARCHAR(255);

-- 2. Populate username with email for existing users
UPDATE users SET username = email WHERE username IS NULL;

-- 3. Make username NOT NULL and UNIQUE
ALTER TABLE users ALTER COLUMN username SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uc_users_username UNIQUE (username);

-- 4. Make email nullable
ALTER TABLE users ALTER COLUMN email SET NULL;

-- 5. Make provider_user_id nullable (as it was NOT NULL in V5)
ALTER TABLE users ALTER COLUMN provider_user_id SET NULL;
