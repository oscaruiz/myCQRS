package com.oscaruiz.mycqrs.core.infrastructure.spring.idempotency;

import com.oscaruiz.mycqrs.core.idempotency.ProcessedCommandsStore;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.UUID;

/**
 * Postgres-backed adapter for {@link ProcessedCommandsStore}.
 *
 * <p>Uses {@code INSERT ... ON CONFLICT (command_id) DO NOTHING} so that a duplicate
 * commandId never aborts the current transaction — a PK-violation-based approach
 * would, and would require savepoints to recover.
 */
public class JdbcProcessedCommandsStore implements ProcessedCommandsStore {

    private static final String INSERT_SQL = """
            INSERT INTO processed_commands (command_id, command_type)
            VALUES (:commandId, :commandType)
            ON CONFLICT (command_id) DO NOTHING
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public JdbcProcessedCommandsStore(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean markProcessedIfAbsent(UUID commandId, String commandType) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("commandId", commandId)
                .addValue("commandType", commandType);
        return jdbc.update(INSERT_SQL, params) == 1;
    }
}
