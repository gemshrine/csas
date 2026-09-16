ALTER TABLE control_results
    ADD CONSTRAINT ck_control_results_numeric_score
        CHECK (numeric_score IS NULL OR (numeric_score >= 0 AND numeric_score <= 100));
