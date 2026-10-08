-- Run this once after the users and job_postings tables exist.
-- The application uses spring.jpa.hibernate.ddl-auto=validate, so it does not
-- create or alter this table automatically.
CREATE TABLE IF NOT EXISTS user_applications (
    application_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    application_status VARCHAR(30) NOT NULL DEFAULT 'APPLIED',
    interview_date DATE NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (application_id),
    CONSTRAINT fk_applications_user
        FOREIGN KEY (user_id)
        REFERENCES users (user_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_applications_job
        FOREIGN KEY (job_id)
        REFERENCES job_postings (job_id)
        ON DELETE CASCADE,
    CONSTRAINT uk_user_application
        UNIQUE (user_id, job_id),
    CONSTRAINT chk_application_status
        CHECK (application_status IN (
            'APPLIED',
            'DOCUMENT_PASS',
            'INTERVIEW',
            'FINAL_PASS',
            'REJECTED'
        ))
);
