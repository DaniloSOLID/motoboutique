package com.motoboutique;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

class MigracionesPostgresTest {

    @Test
    void aplicaTodasLasMigracionesDesdeCero() throws SQLException {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker no está disponible; se omite la prueba de migraciones");
        try (PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine")) {
            postgres.start();
            Flyway.configure()
                    .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
                try (ResultSet result = connection.createStatement().executeQuery("select count(*) from flyway_schema_history where success")) {
                    result.next();
                    org.junit.jupiter.api.Assertions.assertTrue(result.getInt(1) >= 14);
                }
            }
        }
    }
}