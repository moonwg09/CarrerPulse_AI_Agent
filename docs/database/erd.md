erDiagram

    users {
        BIGINT user_id PK "AUTO_INCREMENT"
        VARCHAR_255 email "NOT NULL"
        VARCHAR_100 name "NOT NULL"
        VARCHAR_30 phone
        VARCHAR_20 career_type "NOT NULL"
        DECIMAL_4_1 career_years
        DATE preparation_target_date
        BOOLEAN consented "NOT NULL DEFAULT FALSE"
        VARCHAR_30 consent_version
        DATETIME consented_at
        VARCHAR_20 account_status "NOT NULL DEFAULT ACTIVE"
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        DATETIME updated_at "NOT NULL DEFAULT CURRENT_TIMESTAMP, ON UPDATE CURRENT_TIMESTAMP"
    }

    user_auths {
        BIGINT auth_id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        VARCHAR_20 provider "NOT NULL"
        VARCHAR_255 provider_user_id
        VARCHAR_255 password_hash
        DATETIME email_verified_at
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        DATETIME updated_at "NOT NULL DEFAULT CURRENT_TIMESTAMP, ON UPDATE CURRENT_TIMESTAMP"
    }

    user_jobs {
        BIGINT user_job_id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        VARCHAR_50 job_code "NOT NULL"
        VARCHAR_100 job_name "NOT NULL"
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    user_preferred_companies {
        BIGINT preferred_company_id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        VARCHAR_255 company_name "NOT NULL"
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    user_documents {
        BIGINT document_id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        VARCHAR_30 document_type "NOT NULL"
        VARCHAR_255 original_filename "NOT NULL"
        VARCHAR_1024 storage_path "NOT NULL"
        VARCHAR_10 file_format "NOT NULL"
        BIGINT file_size "NOT NULL"
        LONGTEXT extracted_text
        VARCHAR_20 processing_status "NOT NULL DEFAULT PENDING"
        TEXT processing_error
        DATETIME uploaded_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        DATETIME analyzed_at
    }

    document_chunks {
        BIGINT chunk_id PK "AUTO_INCREMENT"
        BIGINT document_id FK "NOT NULL"
        INT chunk_index "NOT NULL"
        TEXT chunk_text "NOT NULL"
        VARCHAR_255 source_location
        VARCHAR_1024 embedding_ref
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    job_postings {
        BIGINT job_id PK "AUTO_INCREMENT"
        VARCHAR_30 source "NOT NULL"
        VARCHAR_100 external_job_id
        VARCHAR_255 company_name "NOT NULL"
        VARCHAR_255 title "NOT NULL"
        VARCHAR_100 job_category
        VARCHAR_100 region
        LONGTEXT description
        DATE posted_date
        DATE deadline
        VARCHAR_1024 original_url
        VARCHAR_20 status "NOT NULL DEFAULT OPEN"
        DATETIME collected_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        DATETIME updated_at "NOT NULL DEFAULT CURRENT_TIMESTAMP, ON UPDATE CURRENT_TIMESTAMP"
    }

    job_requirements {
        BIGINT requirement_id PK "AUTO_INCREMENT"
        BIGINT job_id FK "NOT NULL"
        VARCHAR_50 requirement_key "NOT NULL"
        VARCHAR_20 requirement_type "NOT NULL"
        VARCHAR_20 comparison_kind "NOT NULL"
        VARCHAR_255 requirement_name "NOT NULL"
        TEXT description
        TEXT source_quote
        VARCHAR_500 evaluation_rule
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    comparison_results {
        BIGINT comparison_id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        BIGINT job_id FK "NOT NULL"
        INT matched_count "NOT NULL DEFAULT 0"
        INT comparable_count "NOT NULL DEFAULT 0"
        INT unknown_count "NOT NULL DEFAULT 0"
        DECIMAL_5_2 match_rate
        VARCHAR_20 analysis_status "NOT NULL DEFAULT PENDING"
        DATETIME analyzed_at
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    comparison_items {
        BIGINT item_id PK "AUTO_INCREMENT"
        BIGINT comparison_id FK "NOT NULL"
        BIGINT requirement_id FK "NOT NULL"
        BIGINT evidence_chunk_id FK
        VARCHAR_30 judgment "NOT NULL"
        TEXT analysis_reason
        BOOLEAN user_confirmed "NOT NULL DEFAULT FALSE"
        VARCHAR_30 user_response_type
        TEXT user_experience_text
        DATETIME rejudged_at
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    learning_plans {
        BIGINT plan_id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        DATE start_date "NOT NULL"
        DATE end_date "NOT NULL"
        DECIMAL_6_2 weekly_available_hours "NOT NULL"
        VARCHAR_20 status "NOT NULL DEFAULT ACTIVE"
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        DATETIME updated_at "NOT NULL DEFAULT CURRENT_TIMESTAMP, ON UPDATE CURRENT_TIMESTAMP"
    }

    learning_plan_items {
        BIGINT item_id PK "AUTO_INCREMENT"
        BIGINT plan_id FK "NOT NULL"
        INT week_number "NOT NULL"
        INT sequence_no "NOT NULL"
        VARCHAR_50 category "NOT NULL"
        VARCHAR_255 task_name "NOT NULL"
        TEXT description
        DECIMAL_6_2 estimated_hours "NOT NULL"
        VARCHAR_20 status "NOT NULL DEFAULT PENDING"
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }



    user_applications {
        BIGINT application_id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        BIGINT job_id FK "NOT NULL"
        VARCHAR_30 application_status "NOT NULL DEFAULT APPLIED"
        DATE interview_date
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        DATETIME updated_at "NOT NULL DEFAULT CURRENT_TIMESTAMP, ON UPDATE CURRENT_TIMESTAMP"
    }

    job_bookmarks {
        BIGINT bookmark_id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        BIGINT job_id FK "NOT NULL"
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    notification_deliveries {
        BIGINT notification_id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        BIGINT job_id FK "NOT NULL"
        VARCHAR_30 notification_type "NOT NULL"
        DATETIME sent_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    company_reviews {
        BIGINT review_id PK "AUTO_INCREMENT"
        BIGINT user_id FK "NOT NULL"
        VARCHAR_255 company_name "NOT NULL"
        VARCHAR_100 job_name "NOT NULL"
        CHAR_7 joined_year_month
        TEXT preparation_tip
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
        DATETIME updated_at "NOT NULL DEFAULT CURRENT_TIMESTAMP, ON UPDATE CURRENT_TIMESTAMP"
    }

    review_interview_questions {
        BIGINT question_id PK "AUTO_INCREMENT"
        BIGINT review_id FK "NOT NULL"
        TEXT question_text "NOT NULL"
        INT question_order "NOT NULL DEFAULT 1"
        DATETIME created_at "NOT NULL DEFAULT CURRENT_TIMESTAMP"
    }

    %% ==========================================
    %% RELATIONSHIPS
    %% ==========================================

    users ||--o{ user_auths : "fk_auth_user / ON DELETE CASCADE"
    users ||--o{ user_jobs : "fk_user_jobs_user / ON DELETE CASCADE"
    users ||--o{ user_preferred_companies : "fk_preferred_companies_user / ON DELETE CASCADE"
    users ||--o{ user_documents : "fk_documents_user / ON DELETE CASCADE"

    user_documents ||--o{ document_chunks : "fk_chunks_document / ON DELETE CASCADE"

    job_postings ||--o{ job_requirements : "fk_requirements_job / ON DELETE CASCADE"

    users ||--o{ comparison_results : "fk_comparison_user"
    job_postings ||--o{ comparison_results : "fk_comparison_job"

    comparison_results ||--o{ comparison_items : "fk_items_comparison / ON DELETE CASCADE"
    job_requirements ||--o{ comparison_items : "fk_items_requirement"
    document_chunks o|--o{ comparison_items : "fk_items_chunk / ON DELETE SET NULL"

    users ||--o{ learning_plans : "fk_plans_user"
    learning_plans ||--o{ learning_plan_items : "fk_plan_items_plan / ON DELETE CASCADE"


    users ||--o{ user_applications : "fk_applications_user / ON DELETE CASCADE"
    job_postings ||--o{ user_applications : "fk_applications_job / ON DELETE CASCADE"
    users ||--o{ job_bookmarks : "fk_bookmarks_user / ON DELETE CASCADE"
    job_postings ||--o{ job_bookmarks : "fk_bookmarks_job / ON DELETE CASCADE"
    users ||--o{ notification_deliveries : "fk_notifications_user / ON DELETE CASCADE"
    job_postings ||--o{ notification_deliveries : "fk_notifications_job / ON DELETE CASCADE"

    users ||--o{ company_reviews : "fk_reviews_user / ON DELETE CASCADE"
    company_reviews ||--o{ review_interview_questions : "fk_review_questions_review / ON DELETE CASCADE"


    %% ==========================================
    %% users CONSTRAINTS
    %% ==========================================
    %% uk_users_email UNIQUE (email)
    %% chk_users_career_type:
    %% career_type IN ('NEW', 'EXPERIENCED')
    %%
    %% chk_users_career_years:
    %% career_years IS NULL OR career_years >= 0
    %%
    %% chk_users_career_required:
    %% career_type = 'NEW' OR career_years IS NOT NULL
    %%
    %% chk_users_consent:
    %% consented = FALSE OR
    %% (consent_version IS NOT NULL AND consented_at IS NOT NULL)


    %% ==========================================
    %% user_auths CONSTRAINTS
    %% ==========================================
    %% uk_user_provider UNIQUE (user_id, provider)
    %% uk_provider_user UNIQUE (provider, provider_user_id)
    %%
    %% chk_auth_provider:
    %% provider IN ('LOCAL', 'GOOGLE')
    %%
    %% chk_auth_credentials:
    %% (
    %%   provider = 'LOCAL'
    %%   AND password_hash IS NOT NULL
    %% )
    %% OR
    %% (
    %%   provider = 'GOOGLE'
    %%   AND provider_user_id IS NOT NULL
    %%   AND password_hash IS NULL
    %% )


    %% ==========================================
    %% user_jobs CONSTRAINTS
    %% ==========================================
    %% uk_user_job UNIQUE (user_id, job_code)

    %% ==========================================
    %% user_preferred_companies CONSTRAINTS
    %% ==========================================
    %% uk_user_preferred_company UNIQUE (user_id, company_name)
    %% 관심 기업 최대 3개 제한은 서비스 계층에서 검증


    %% ==========================================
    %% user_documents CONSTRAINTS
    %% ==========================================
    %% uk_user_document_type UNIQUE (user_id, document_type)
    %%
    %% chk_document_type:
    %% document_type IN ('RESUME', 'SELF_INTRO', 'PORTFOLIO')
    %%
    %% chk_document_format:
    %% file_format IN ('PDF', 'DOCX')
    %%
    %% chk_document_size:
    %% file_size > 0 AND file_size <= 10485760
    %%
    %% chk_document_status:
    %% processing_status IN
    %% ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')


    %% ==========================================
    %% document_chunks CONSTRAINTS
    %% ==========================================
    %% uk_document_chunk UNIQUE (document_id, chunk_index)
    %%
    %% chk_chunk_index:
    %% chunk_index >= 0


    %% ==========================================
    %% job_postings CONSTRAINTS
    %% ==========================================
    %% uk_external_job UNIQUE (source, external_job_id)
    %%
    %% chk_job_status:
    %% status IN ('OPEN', 'CLOSED', 'UNKNOWN')


    %% ==========================================
    %% job_requirements CONSTRAINTS
    %% ==========================================
    %% uk_job_requirement UNIQUE (job_id, requirement_key)
    %%
    %% chk_requirement_type:
    %% requirement_type IN ('REQUIRED', 'PREFERRED', 'UNCLEAR')
    %%
    %% chk_comparison_kind:
    %% comparison_kind IN ('EXPERIENCE', 'OBJECTIVE')


    %% ==========================================
    %% comparison_results CONSTRAINTS
    %% ==========================================
    %% chk_comparison_counts:
    %% matched_count >= 0
    %% AND comparable_count >= 0
    %% AND unknown_count >= 0
    %% AND matched_count <= comparable_count
    %%
    %% chk_match_rate:
    %% match_rate IS NULL
    %% OR (match_rate >= 0 AND match_rate <= 100)
    %%
    %% chk_analysis_status:
    %% analysis_status IN
    %% ('PENDING', 'COMPLETED', 'FAILED', 'INVALIDATED')
    %%
    %% INDEX idx_comparison_user_job (user_id, job_id)


    %% ==========================================
    %% comparison_items CONSTRAINTS
    %% ==========================================
    %% uk_comparison_requirement
    %% UNIQUE (comparison_id, requirement_id)
    %%
    %% chk_item_judgment:
    %% judgment IN (
    %% 'EXPERIENCED',
    %% 'PARTIAL',
    %% 'INSUFFICIENT',
    %% 'NO_EXPERIENCE',
    %% 'MET',
    %% 'UNMET',
    %% 'UNKNOWN'
    %% )
    %%
    %% chk_no_experience_confirmation:
    %% judgment <> 'NO_EXPERIENCE'
    %% OR user_confirmed = TRUE
    %%
    %% chk_user_response_type:
    %% user_response_type IS NULL OR user_response_type IN (
    %% 'EXPERIENCED', 'PARTIAL', 'NO_EXPERIENCE'
    %% )


    %% ==========================================
    %% learning_plans CONSTRAINTS
    %% ==========================================
    %% chk_plan_dates:
    %% end_date >= start_date
    %%
    %% chk_weekly_hours:
    %% weekly_available_hours > 0
    %%
    %% chk_plan_status:
    %% status IN
    %% ('ACTIVE', 'COMPLETED', 'CANCELLED', 'INVALIDATED')


    %% ==========================================
    %% learning_plan_items CONSTRAINTS
    %% ==========================================
    %% uk_plan_week_sequence
    %% UNIQUE (plan_id, week_number, sequence_no)
    %%
    %% chk_plan_week:
    %% week_number >= 1
    %%
    %% chk_estimated_hours:
    %% estimated_hours > 0
    %%
    %% chk_plan_item_status:
    %% status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')

    %% ==========================================
    %% user_applications CONSTRAINTS
    %% ==========================================
    %% uk_user_application UNIQUE (user_id, job_id)
    %%
    %% chk_application_status:
    %% application_status IN
    %% ('APPLIED', 'DOCUMENT_PASS', 'INTERVIEW', 'FINAL_PASS', 'REJECTED')

    %% ==========================================
    %% job_bookmarks CONSTRAINTS
    %% ==========================================
    %% uk_user_job_bookmark UNIQUE (user_id, job_id)

    %% ==========================================
    %% notification_deliveries CONSTRAINTS
    %% ==========================================
    %% uk_notification_delivery
    %% UNIQUE (user_id, job_id, notification_type)
    %%
    %% chk_notification_type:
    %% notification_type IN ('NEW_RECOMMENDATION', 'DEADLINE_REMINDER')


    %% ==========================================
    %% company_reviews INDEXES
    %% ==========================================
    %% INDEX idx_company_reviews_company_name (company_name)


    %% ==========================================
    %% review_interview_questions CONSTRAINTS
    %% ==========================================
    %% chk_question_order:
    %% question_order >= 1


