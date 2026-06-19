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
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expired_at TIMESTAMP WITH TIME ZONE
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

CREATE TABLE IF NOT EXISTS system_settings (
    setting_key VARCHAR(50) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS route_rules (
    id VARCHAR(50) PRIMARY KEY,
    link_id VARCHAR(50) NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    rule_type VARCHAR(50) NOT NULL,
    rule_value VARCHAR(100) NOT NULL,
    target_url TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS settlements (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    amount INTEGER NOT NULL,
    status VARCHAR(50) NOT NULL,
    settled_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    bank_name VARCHAR(100),
    account_number VARCHAR(100),
    account_holder VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS payments (
    id VARCHAR(50) PRIMARY KEY,
    link_id VARCHAR(50) NOT NULL REFERENCES links(id) ON DELETE CASCADE,
    ip_hash VARCHAR(64) NOT NULL,
    amount INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
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

INSERT INTO users (id, email, subscription_tier, subscription_ends_at)
VALUES ('anonymous', 'anonymous@pixel-link.com', 'FREE', NULL)
ON CONFLICT (id) DO NOTHING;

-- 초기 설정값 등록
INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('anon_link_expiry_days', '30', '비회원 단축 링크 만료 기간 (일)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('ad_reward_per_click', '70', '광고 클릭당 적립 단가 (원)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('min_withdrawal_amount', '10000', '최소 출금 신청 금액 (원)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('starter_monthly_fee', '9900', '스타터 요금제 가격 (원)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('premium_monthly_fee', '19900', '프리미엄 요금제 가격 (원)')
ON CONFLICT (setting_key) DO NOTHING;

CREATE TABLE IF NOT EXISTS api_keys (
    id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL REFERENCES users(id),
    api_key VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_api_keys_token ON api_keys(api_key);

