CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(50) PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    name VARCHAR(100),
    phone VARCHAR(50),
    terms_agreed BOOLEAN DEFAULT FALSE,
    subscription_tier VARCHAR(50) DEFAULT 'FREE',
    subscription_ends_at TIMESTAMP WITH TIME ZONE,
    role VARCHAR(50) DEFAULT 'USER'
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

CREATE TABLE IF NOT EXISTS system_audit_logs (
    id VARCHAR(50) PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    actor_id VARCHAR(50),
    ip_address VARCHAR(50),
    user_agent TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
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
VALUES ('is_installed', 'false', '최초 시스템 설치 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('oauth_google_enabled', 'true', 'Google 소셜 로그인 노출 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('oauth_facebook_enabled', 'true', 'Facebook 소셜 로그인 노출 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('oauth_naver_enabled', 'true', 'Naver 소셜 로그인 노출 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('oauth_kakao_enabled', 'true', 'Kakao 소셜 로그인 노출 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;

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

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('feature_custom_slug_enabled', 'true', 'Custom Slug 기능 활성화 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('feature_seo_preview_enabled', 'true', 'SEO Social Preview 기능 활성화 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('feature_smart_routing_enabled', 'true', '스마트 라우팅 기능 활성화 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('feature_monetization_enabled', 'true', '수익화 기능 활성화 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('feature_marketing_pixel_enabled', 'true', '마케팅 픽셀 기능 활성화 여부 (true/false)')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('board_notice_comment_policy', 'ADMIN_ONLY', '공지사항 댓글 쓰기 정책')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('board_free_comment_policy', 'ALL', '자유게시판 댓글 쓰기 정책')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('board_qna_comment_policy', 'OWNER_AND_ADMIN', '1:1 문의 댓글 쓰기 정책')
ON CONFLICT (setting_key) DO NOTHING;

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('board_partnership_comment_policy', 'OWNER_AND_ADMIN', '제휴제안 댓글 쓰기 정책')
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

-- 기존 가동 중인 users 테이블 업데이트를 위한 DDL
ALTER TABLE users ADD COLUMN IF NOT EXISTS password VARCHAR(255);
ALTER TABLE users ADD COLUMN IF NOT EXISTS name VARCHAR(100);
ALTER TABLE users ADD COLUMN IF NOT EXISTS phone VARCHAR(50);
ALTER TABLE users ADD COLUMN IF NOT EXISTS terms_agreed BOOLEAN DEFAULT FALSE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS role VARCHAR(50) DEFAULT 'USER';
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

CREATE TABLE IF NOT EXISTS board_articles (
    id VARCHAR(50) PRIMARY KEY,
    board_type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author_id VARCHAR(50) REFERENCES users(id),
    author_name VARCHAR(100) NOT NULL,
    is_secret BOOLEAN DEFAULT FALSE,
    password VARCHAR(255),
    status VARCHAR(50) DEFAULT 'OPEN',
    view_count INTEGER DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS board_comments (
    id VARCHAR(50) PRIMARY KEY,
    article_id VARCHAR(50) NOT NULL REFERENCES board_articles(id) ON DELETE CASCADE,
    author_id VARCHAR(50) REFERENCES users(id),
    author_name VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    is_admin_reply BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS board_attachments (
    id VARCHAR(50) PRIMARY KEY,
    article_id VARCHAR(50) NOT NULL REFERENCES board_articles(id) ON DELETE CASCADE,
    original_filename VARCHAR(255) NOT NULL,
    stored_filename VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

