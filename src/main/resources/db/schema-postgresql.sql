CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    subscription_tier VARCHAR(50) DEFAULT 'FREE',
    subscription_ends_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE IF NOT EXISTS links (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    short_code VARCHAR(100) UNIQUE NOT NULL,
    default_target_url TEXT NOT NULL,
    title VARCHAR(255),
    description TEXT,
    og_image TEXT,
    fb_pixel_id VARCHAR(50),
    ga_tracking_id VARCHAR(50),
    custom_script TEXT,
    is_ad_enabled BOOLEAN DEFAULT FALSE,
    ad_timer_seconds INTEGER DEFAULT 1,
    is_paywalled BOOLEAN DEFAULT FALSE,
    price INTEGER DEFAULT 0,
    clicks_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS click_logs (
    id VARCHAR(50) PRIMARY KEY,
    link_id VARCHAR(50) NOT NULL REFERENCES links(id),
    user_agent TEXT,
    ip_hash VARCHAR(64) NOT NULL,
    referrer TEXT,
    device_type VARCHAR(50),
    os_type VARCHAR(50),
    is_ad_clicked BOOLEAN DEFAULT FALSE,
    is_converted BOOLEAN DEFAULT FALSE,
    timestamp TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 초기 마스터 회원 등록
INSERT INTO users (id, email, subscription_tier, subscription_ends_at)
VALUES ('admin', 'admin@pixel-link.com', 'PREMIUM', '2030-12-31T23:59:59+09:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, subscription_tier, subscription_ends_at)
VALUES ('test-user', 'user@pixel-link.com', 'STARTER', '2030-12-31T23:59:59+09:00')
ON CONFLICT (id) DO NOTHING;

INSERT INTO users (id, email, subscription_tier, subscription_ends_at)
VALUES ('free-user', 'free@pixel-link.com', 'FREE', NULL)
ON CONFLICT (id) DO NOTHING;
