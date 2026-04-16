-- Outbox table for reliable event publication. See docs/adr/0003-outbox-pattern.md.
-- Events are written here within the same JPA transaction as the aggregate,
-- then published asynchronously by OutboxPoller.

CREATE TABLE outbox (
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

CREATE INDEX idx_outbox_pending ON outbox (processed_at, occurred_at);
