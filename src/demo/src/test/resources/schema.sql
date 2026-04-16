-- Non-JPA tables needed for tests. Hibernate create-drop only creates
-- JPA-mapped entities; tables managed by Flyway (like outbox) must be
-- created explicitly in the test profile where Flyway is disabled.

CREATE TABLE IF NOT EXISTS outbox (
    id UUID NOT NULL,
    aggregate_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    attempts INT NOT NULL DEFAULT 0,
    last_error TEXT,
    PRIMARY KEY (id)
);
