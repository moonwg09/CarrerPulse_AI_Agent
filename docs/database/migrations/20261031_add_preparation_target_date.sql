-- Apply once to both careerpulse_mvp_dev and careerpulse_test.
ALTER TABLE users
    ADD COLUMN preparation_target_date DATE NULL AFTER career_years;
