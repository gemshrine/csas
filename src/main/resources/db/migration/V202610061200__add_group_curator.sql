ALTER TABLE groups
    ADD COLUMN curator_id BIGINT;

ALTER TABLE groups
    ADD CONSTRAINT fk_groups_curator
        FOREIGN KEY (curator_id) REFERENCES users (id);

CREATE INDEX ix_groups_curator_id ON groups (curator_id);
