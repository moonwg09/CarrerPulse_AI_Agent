CREATE TABLE IF NOT EXISTS interview_question (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    category      VARCHAR(50)  NOT NULL,
    question_type VARCHAR(30)  NOT NULL,
    content       VARCHAR(500) NOT NULL,
    PRIMARY KEY (id)
) DEFAULT CHARSET = utf8mb4;
