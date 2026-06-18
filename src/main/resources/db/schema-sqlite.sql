CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    email TEXT UNIQUE NOT NULL,
    subscription_tier TEXT DEFAULT 'FREE',
    subscription_ends_at TEXT
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

-- 초기 마스터 회원 등록 (1단계 SaaS 구독 기능 테스트를 위한 기본 계정)
INSERT OR IGNORE INTO users (id, email, subscription_tier, subscription_ends_at)
VALUES ('admin', 'admin@pixel-link.com', 'PREMIUM', '2030-12-31T23:59:59');

INSERT OR IGNORE INTO users (id, email, subscription_tier, subscription_ends_at)
VALUES ('test-user', 'user@pixel-link.com', 'STARTER', '2030-12-31T23:59:59');

INSERT OR IGNORE INTO users (id, email, subscription_tier, subscription_ends_at)
VALUES ('free-user', 'free@pixel-link.com', 'FREE', NULL);
