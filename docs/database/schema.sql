-- CareerPulse schema
-- Generated from erd.md
-- Target: MySQL 8.x
-- Database-neutral DDL: select the target database before executing this file.



CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(30),
    career_type VARCHAR(20) NOT NULL,
    career_years DECIMAL(4,1),
    consented BOOLEAN NOT NULL DEFAULT FALSE,
    consent_version VARCHAR(30),
    consented_at DATETIME,
    account_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT chk_users_career_type CHECK (career_type IN ('NEW', 'EXPERIENCED')),
    CONSTRAINT chk_users_career_years CHECK (career_years IS NULL OR career_years >= 0),
    CONSTRAINT chk_users_career_required CHECK (career_type = 'NEW' OR career_years IS NOT NULL),
    CONSTRAINT chk_users_consent CHECK (
        consented = FALSE OR
        (consent_version IS NOT NULL AND consented_at IS NOT NULL)
    )
) ENGINE=InnoDB;

CREATE TABLE user_auths (
    auth_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_user_id VARCHAR(255),
    password_hash VARCHAR(255),
    email_verified_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_provider UNIQUE (user_id, provider),
    CONSTRAINT uk_provider_user UNIQUE (provider, provider_user_id),
    CONSTRAINT chk_auth_provider CHECK (provider IN ('LOCAL', 'GOOGLE')),
    CONSTRAINT chk_auth_credentials CHECK (
        (provider = 'LOCAL' AND password_hash IS NOT NULL)
        OR
        (provider = 'GOOGLE' AND provider_user_id IS NOT NULL AND password_hash IS NULL)
    ),
    CONSTRAINT fk_auth_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_jobs (
    user_job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_code VARCHAR(50) NOT NULL,
    job_name VARCHAR(100) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_job UNIQUE (user_id, job_code),
    CONSTRAINT fk_user_jobs_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_documents (
    document_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    document_type VARCHAR(30) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_path VARCHAR(1024) NOT NULL,
    file_format VARCHAR(10) NOT NULL,
    file_size BIGINT NOT NULL,
    extracted_text LONGTEXT,
    processing_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    uploaded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    analyzed_at DATETIME,

    CONSTRAINT uk_user_document_type UNIQUE (user_id, document_type),
    CONSTRAINT chk_document_type CHECK (
        document_type IN ('RESUME', 'SELF_INTRO', 'PORTFOLIO')
    ),
    CONSTRAINT chk_document_format CHECK (file_format IN ('PDF', 'DOCX')),
    CONSTRAINT chk_document_size CHECK (
        file_size > 0 AND file_size <= 10485760
    ),
    CONSTRAINT chk_document_status CHECK (
        processing_status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')
    ),
    CONSTRAINT fk_documents_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE document_chunks (
    chunk_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    chunk_text TEXT NOT NULL,
    source_location VARCHAR(255),
    embedding_ref VARCHAR(1024),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_document_chunk UNIQUE (document_id, chunk_index),
    CONSTRAINT chk_chunk_index CHECK (chunk_index >= 0),
    CONSTRAINT fk_chunks_document FOREIGN KEY (document_id)
        REFERENCES user_documents(document_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE job_postings (
    job_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source VARCHAR(30) NOT NULL,
    external_job_id VARCHAR(100),
    company_name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    job_category VARCHAR(100),
    region VARCHAR(100),
    description LONGTEXT,
    posted_date DATE,
    deadline DATE,
    original_url VARCHAR(1024),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    collected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_external_job UNIQUE (source, external_job_id),
    CONSTRAINT chk_job_status CHECK (status IN ('OPEN', 'CLOSED', 'UNKNOWN'))
) ENGINE=InnoDB;

CREATE TABLE job_requirements (
    requirement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    requirement_key VARCHAR(50) NOT NULL,
    requirement_type VARCHAR(20) NOT NULL,
    comparison_kind VARCHAR(20) NOT NULL,
    requirement_name VARCHAR(255) NOT NULL,
    description TEXT,
    source_quote TEXT,
    evaluation_rule VARCHAR(500),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_job_requirement UNIQUE (job_id, requirement_key),
    CONSTRAINT chk_requirement_type CHECK (
        requirement_type IN ('REQUIRED', 'PREFERRED')
    ),
    CONSTRAINT chk_comparison_kind CHECK (
        comparison_kind IN ('EXPERIENCE', 'OBJECTIVE')
    ),
    CONSTRAINT fk_requirements_job FOREIGN KEY (job_id)
        REFERENCES job_postings(job_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE comparison_results (
    comparison_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    matched_count INT NOT NULL DEFAULT 0,
    comparable_count INT NOT NULL DEFAULT 0,
    unknown_count INT NOT NULL DEFAULT 0,
    match_rate DECIMAL(5,2),
    analysis_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    analyzed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_comparison_counts CHECK (
        matched_count >= 0
        AND comparable_count >= 0
        AND unknown_count >= 0
        AND matched_count <= comparable_count
    ),
    CONSTRAINT chk_match_rate CHECK (
        match_rate IS NULL OR (match_rate >= 0 AND match_rate <= 100)
    ),
    CONSTRAINT chk_analysis_status CHECK (
        analysis_status IN ('PENDING', 'COMPLETED', 'FAILED', 'INVALIDATED')
    ),
    CONSTRAINT fk_comparison_user FOREIGN KEY (user_id)
        REFERENCES users(user_id),
    CONSTRAINT fk_comparison_job FOREIGN KEY (job_id)
        REFERENCES job_postings(job_id),

    INDEX idx_comparison_user_job (user_id, job_id)
) ENGINE=InnoDB;

CREATE TABLE comparison_items (
    item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    comparison_id BIGINT NOT NULL,
    requirement_id BIGINT NOT NULL,
    evidence_chunk_id BIGINT,
    judgment VARCHAR(30) NOT NULL,
    analysis_reason TEXT,
    user_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_comparison_requirement UNIQUE (comparison_id, requirement_id),
    CONSTRAINT chk_item_judgment CHECK (
        judgment IN (
            'EXPERIENCED',
            'PARTIAL',
            'INSUFFICIENT',
            'NO_EXPERIENCE',
            'MET',
            'UNMET',
            'UNKNOWN'
        )
    ),
    CONSTRAINT chk_no_experience_confirmation CHECK (
        judgment <> 'NO_EXPERIENCE' OR user_confirmed = TRUE
    ),
    CONSTRAINT fk_items_comparison FOREIGN KEY (comparison_id)
        REFERENCES comparison_results(comparison_id) ON DELETE CASCADE,
    CONSTRAINT fk_items_requirement FOREIGN KEY (requirement_id)
        REFERENCES job_requirements(requirement_id),
    CONSTRAINT fk_items_chunk FOREIGN KEY (evidence_chunk_id)
        REFERENCES document_chunks(chunk_id) ON DELETE SET NULL
) ENGINE=InnoDB;

CREATE TABLE learning_plans (
    plan_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    weekly_available_hours DECIMAL(6,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_plan_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_weekly_hours CHECK (weekly_available_hours > 0),
    CONSTRAINT chk_plan_status CHECK (
        status IN ('ACTIVE', 'COMPLETED', 'CANCELLED', 'INVALIDATED')
    ),
    CONSTRAINT fk_plans_user FOREIGN KEY (user_id)
        REFERENCES users(user_id)
) ENGINE=InnoDB;

CREATE TABLE learning_plan_items (
    item_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    plan_id BIGINT NOT NULL,
    week_number INT NOT NULL,
    sequence_no INT NOT NULL,
    category VARCHAR(50) NOT NULL,
    task_name VARCHAR(255) NOT NULL,
    description TEXT,
    estimated_hours DECIMAL(6,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uk_plan_week_sequence UNIQUE (plan_id, week_number, sequence_no),
    CONSTRAINT chk_plan_week CHECK (week_number >= 1),
    CONSTRAINT chk_estimated_hours CHECK (estimated_hours > 0),
    CONSTRAINT chk_plan_item_status CHECK (
        status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')
    ),
    CONSTRAINT fk_plan_items_plan FOREIGN KEY (plan_id)
        REFERENCES learning_plans(plan_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE user_applications (
    application_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    application_status VARCHAR(30) NOT NULL DEFAULT 'APPLIED',
    interview_date DATE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT uk_user_application UNIQUE (user_id, job_id),
    CONSTRAINT chk_application_status CHECK (
        application_status IN ('APPLIED', 'DOCUMENT_PASS', 'INTERVIEW', 'FINAL_PASS', 'REJECTED')
    ),
    CONSTRAINT fk_applications_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_applications_job FOREIGN KEY (job_id)
        REFERENCES job_postings(job_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE company_reviews (
    review_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    job_name VARCHAR(100) NOT NULL,
    joined_year_month CHAR(7),
    preparation_tip TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_reviews_user FOREIGN KEY (user_id)
        REFERENCES users(user_id) ON DELETE CASCADE,

    INDEX idx_company_reviews_company_name (company_name)
) ENGINE=InnoDB;

CREATE TABLE review_interview_questions (
    question_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id BIGINT NOT NULL,
    question_text TEXT NOT NULL,
    question_order INT NOT NULL DEFAULT 1,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_question_order CHECK (question_order >= 1),
    CONSTRAINT fk_review_questions_review FOREIGN KEY (review_id)
        REFERENCES company_reviews(review_id) ON DELETE CASCADE
) ENGINE=InnoDB;


