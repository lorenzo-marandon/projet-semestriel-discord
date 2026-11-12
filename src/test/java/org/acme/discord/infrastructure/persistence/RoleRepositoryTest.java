package org.acme.discord.infrastructure.persistence;

import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoleRepositoryTest {

    private final RoleRepository repository = new RoleRepository();

    @Test
    void shouldRecognizeOnlyUserRoleForeignKeyViolations() {
        assertTrue(repository.isUserRoleForeignKeyViolation(
                wrappedSqlException("Foreign key violation on table user_roles", "23503")));

        assertFalse(repository.isUserRoleForeignKeyViolation(
                wrappedSqlException("Foreign key violation on another_table", "23503")));
        assertFalse(repository.isUserRoleForeignKeyViolation(
                wrappedSqlException("Unique violation on table user_roles", "23505")));
        assertFalse(repository.isUserRoleForeignKeyViolation(new PersistenceException("No SQL cause")));
    }

    private static PersistenceException wrappedSqlException(String message, String sqlState) {
        return new PersistenceException(new SQLException(message, sqlState));
    }
}
