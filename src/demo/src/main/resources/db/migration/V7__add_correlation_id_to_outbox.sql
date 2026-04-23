ALTER TABLE outbox ADD COLUMN correlation_id UUID;
CREATE INDEX idx_outbox_correlation_id ON outbox(correlation_id);
