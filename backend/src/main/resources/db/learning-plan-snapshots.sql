-- 기존 계획/항목 테이블은 변경하지 않는다. 생성 당시 입력과 결과 전체를 보존한다.
-- PLN-05 조회는 Gemini를 재호출하지 않고 저장된 동일 계획을 반환한다.
CREATE TABLE IF NOT EXISTS learning_plan_snapshots (
    plan_id BIGINT NOT NULL PRIMARY KEY,
    snapshot JSON NOT NULL,
    CONSTRAINT fk_learning_snapshot_plan FOREIGN KEY (plan_id)
        REFERENCES learning_plans(plan_id) ON DELETE CASCADE
);
