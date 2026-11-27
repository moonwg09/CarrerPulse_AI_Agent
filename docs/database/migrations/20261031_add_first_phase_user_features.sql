-- 1차/1차-A/1차-B 사용자 기능 보완
-- 실행 전 대상 DB를 명시적으로 선택해야 한다.

ALTER TABLE comparison_items
    ADD COLUMN user_response_type VARCHAR(30) NULL AFTER user_confirmed,
    ADD COLUMN user_experience_text TEXT NULL AFTER user_response_type,
    ADD COLUMN rejudged_at DATETIME NULL AFTER user_experience_text,
    ADD CONSTRAINT chk_user_response_type CHECK (
        user_response_type IS NULL OR user_response_type IN (
            'EXPERIENCED', 'PARTIAL', 'NO_EXPERIENCE'
        )
    );

CREATE TABLE user_preferred_companies (
    preferred_company_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_preferred_company UNIQUE (user_id, company_name),
    CONSTRAINT fk_preferred_companies_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE job_bookmarks (
    bookmark_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_job_bookmark UNIQUE (user_id, job_id),
    CONSTRAINT fk_bookmarks_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_bookmarks_job FOREIGN KEY (job_id)
        REFERENCES job_postings(job_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE notification_deliveries (
    notification_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    notification_type VARCHAR(30) NOT NULL,
    sent_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_delivery UNIQUE (user_id, job_id, notification_type),
    CONSTRAINT chk_notification_type CHECK (
        notification_type IN ('NEW_RECOMMENDATION', 'DEADLINE_REMINDER')
    ),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_job FOREIGN KEY (job_id)
        REFERENCES job_postings(job_id) ON DELETE CASCADE
) ENGINE=InnoDB;
