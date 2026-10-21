-- analysis 기능 전용 테이블
-- [서류와 공고비교] 결과 저장 / [추천조회] 에 사용합니다.
--
-- 이 프로젝트는 spring.jpa.hibernate.ddl-auto=validate 를 사용하므로
-- 애플리케이션이 테이블을 자동으로 생성하거나 변경하지 않습니다.
-- careerpulse_mvp_dev 스키마에 아래 스크립트를 한 번 실행해 주세요.
--
-- 선행 조건: users 테이블이 먼저 존재해야 합니다.

CREATE TABLE IF NOT EXISTS analysis_results (
    analysis_id           BIGINT        NOT NULL AUTO_INCREMENT,
    user_id               BIGINT        NOT NULL,

    -- 공고 식별 키. 현재 main 에는 job_postings 테이블이 없으므로
    -- FastAPI 가 사용하는 jobId 문자열을 그대로 보관한다.
    job_key               VARCHAR(100)  NOT NULL,
    job_title             VARCHAR(200)  NULL,
    company_name          VARCHAR(200)  NULL,
    close_date            DATE          NULL,

    -- RECOMMENDED / GROWTH_CANDIDATE / EXCLUDED
    recommendation_status VARCHAR(30)   NULL,

    overall_match_rate    DECIMAL(5,2)  NULL,
    required_match_rate   DECIMAL(5,2)  NULL,
    preferred_match_rate  DECIMAL(5,2)  NULL,
    objective_match_rate  DECIMAL(5,2)  NULL,
    evaluation_coverage   DECIMAL(5,2)  NULL,

    -- AI 가 반환한 경고 문자열 배열(JSON)
    warnings              LONGTEXT      NULL,

    -- FastAPI 응답 원본(JSON). 근거 원문을 손실 없이 보관한다.
    raw_result            LONGTEXT      NOT NULL,

    created_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
                                        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (analysis_id),

    KEY idx_analysis_results_user_status
        (user_id, recommendation_status, close_date),

    CONSTRAINT fk_analysis_results_user
        FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE,

    -- 같은 사용자가 같은 공고를 다시 비교하면 기존 결과를 갱신한다.
    CONSTRAINT uk_analysis_user_job
        UNIQUE (user_id, job_key),

    CONSTRAINT chk_analysis_recommendation_status
        CHECK (recommendation_status IS NULL
               OR recommendation_status IN (
                    'RECOMMENDED',
                    'GROWTH_CANDIDATE',
                    'EXCLUDED'
               ))
) DEFAULT CHARSET = utf8mb4;


CREATE TABLE IF NOT EXISTS analysis_items (
    item_id              BIGINT        NOT NULL AUTO_INCREMENT,
    analysis_id          BIGINT        NOT NULL,

    item_order           INT           NOT NULL DEFAULT 0,

    requirement          VARCHAR(500)  NOT NULL,

    -- REQUIRED / PREFERRED / UNCLEAR
    requirement_category VARCHAR(20)   NOT NULL,

    -- EXPERIENCED / PARTIAL / INSUFFICIENT_EVIDENCE / NO_EXPERIENCE
    match_status         VARCHAR(30)   NOT NULL,

    reason               VARCHAR(1000) NULL,

    matched_skills       LONGTEXT      NULL,
    missing_skills       LONGTEXT      NULL,

    -- 이력서/자기소개서 근거 원문(JSON)
    user_evidence        LONGTEXT      NULL,

    -- 공고 근거 원문(JSON)
    job_evidence         LONGTEXT      NULL,

    PRIMARY KEY (item_id),

    KEY idx_analysis_items_analysis (analysis_id),

    CONSTRAINT fk_analysis_items_result
        FOREIGN KEY (analysis_id)
        REFERENCES analysis_results (analysis_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_analysis_item_category
        CHECK (requirement_category IN (
                    'REQUIRED',
                    'PREFERRED',
                    'UNCLEAR'
               )),

    CONSTRAINT chk_analysis_item_status
        CHECK (match_status IN (
                    'EXPERIENCED',
                    'PARTIAL',
                    'INSUFFICIENT_EVIDENCE',
                    'NO_EXPERIENCE'
               ))
) DEFAULT CHARSET = utf8mb4;
