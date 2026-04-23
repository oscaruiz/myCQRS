-- Command-level idempotency ledger. See docs/adr/0011-command-level-idempotency.md.
-- IdempotencyCommandInterceptor inserts here with ON CONFLICT DO NOTHING inside
-- the command transaction, so handler side effects and the ledger row commit or
-- roll back together.

CREATE TABLE processed_commands (
    command_id   UUID         NOT NULL,
    command_type VARCHAR(255) NOT NULL,
    processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    PRIMARY KEY (command_id)
);

CREATE INDEX idx_processed_commands_processed_at ON processed_commands (processed_at);
