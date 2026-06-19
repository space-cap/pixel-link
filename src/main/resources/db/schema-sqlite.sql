CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    subscription_tier TEXT DEFAULT 'FREE',
    subscription_ends_at TEXT,
    role TEXT DEFAULT 'USER'
);

CREATE TABLE IF NOT EXISTS links (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    short_code TEXT UNIQUE NOT NULL,
    default_target_url TEXT NOT NULL,
    title TEXT,
    description TEXT,
    og_image TEXT,
    fb_pixel_id TEXT,
    ga_tracking_id TEXT,
    custom_script TEXT,
    is_ad_enabled INTEGER DEFAULT 0,
    ad_timer_seconds INTEGER DEFAULT 1,
    is_paywalled INTEGER DEFAULT 0,
    price INTEGER DEFAULT 0,
    clicks_count INTEGER DEFAULT 0,
    created_at TEXT DEFAULT (datetime('now', 'localtime')),
    expired_at TEXT,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS click_logs (
    id TEXT PRIMARY KEY,
    link_id TEXT NOT NULL,
    user_agent TEXT,
    ip_hash TEXT NOT NULL,
    referrer TEXT,
    device_type TEXT,
    os_type TEXT,
    is_ad_clicked INTEGER DEFAULT 0,
    is_converted INTEGER DEFAULT 0,
    timestamp TEXT DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(link_id) REFERENCES links(id)
);

CREATE TABLE IF NOT EXISTS system_settings (
    setting_key TEXT PRIMARY KEY,
    setting_value TEXT NOT NULL,
    description TEXT
);

CREATE TABLE IF NOT EXISTS route_rules (
    id TEXT PRIMARY KEY,
    link_id TEXT NOT NULL,
    rule_type TEXT NOT NULL,
    rule_value TEXT NOT NULL,
    target_url TEXT NOT NULL,
    FOREIGN KEY(link_id) REFERENCES links(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS settlements (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    amount INTEGER NOT NULL,
    status TEXT NOT NULL,
    settled_at TEXT DEFAULT (datetime('now', 'localtime')),
    bank_name TEXT,
    account_number TEXT,
    account_holder TEXT,
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS payments (
    id TEXT PRIMARY KEY,
    link_id TEXT NOT NULL,
    ip_hash TEXT NOT NULL,
    amount INTEGER NOT NULL,
    created_at TEXT DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(link_id) REFERENCES links(id) ON DELETE CASCADE
);


-- 초기 마스터 회원 등록 (1단계 SaaS 구독 기능 테스트를 위한 기본 계정)
INSERT OR IGNORE INTO users (id, email, subscription_tier, subscription_ends_at, role)
VALUES ('admin', 'admin@pixel-link.com', 'PREMIUM', '2030-12-31T23:59:59', 'ADMIN');

INSERT OR IGNORE INTO users (id, email, subscription_tier, subscription_ends_at, role)
VALUES ('test-user', 'user@pixel-link.com', 'STARTER', '2030-12-31T23:59:59', 'USER');

INSERT OR IGNORE INTO users (id, email, subscription_tier, subscription_ends_at, role)
VALUES ('free-user', 'free@pixel-link.com', 'FREE', NULL, 'USER');

INSERT OR IGNORE INTO users (id, email, subscription_tier, subscription_ends_at, role)
VALUES ('anonymous', 'anonymous@pixel-link.com', 'FREE', NULL, 'USER');

-- 초기 설정값 등록
INSERT OR IGNORE INTO system_settings (setting_key, setting_value, description)
VALUES ('anon_link_expiry_days', '30', '비회원 단축 링크 만료 기간 (일)');

CREATE TABLE IF NOT EXISTS api_keys (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    api_key TEXT UNIQUE NOT NULL,
    name TEXT,
    is_active INTEGER DEFAULT 1,
    created_at TEXT DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_api_keys_token ON api_keys(api_key);

