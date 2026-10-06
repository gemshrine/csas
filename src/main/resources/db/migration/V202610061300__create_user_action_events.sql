CREATE TABLE user_action_events (
    id BIGSERIAL PRIMARY KEY,
    actor_user_id BIGINT,
    actor_username VARCHAR(50) NOT NULL,
    actor_role VARCHAR(20) NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    details VARCHAR(1000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_action_events_actor FOREIGN KEY (actor_user_id)
        REFERENCES users (id) ON DELETE SET NULL
);

CREATE INDEX ix_user_action_events_created_at_id ON user_action_events (created_at DESC, id DESC);
