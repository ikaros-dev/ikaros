package run.ikaros.server.core.migration;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import java.io.IOException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.test.StepVerifier;

/**
 * Test to verify PostgreSQL connection using manually started Docker container.
 * This bypasses Testcontainers Docker detection issues.
 */
class TestcontainersConnectionTest {

    private static final String CONTAINER_NAME = "ikaros-test-pg";
    private static final String PG_HOST = "localhost";
    private static final int PG_PORT = 5433;
    private static final String PG_DATABASE = "testdb";
    private static final String PG_USER = "test";
    private static final String PG_PASSWORD = "test";

    @BeforeAll
    static void startContainer() throws IOException, InterruptedException {
        // Stop and remove container if it exists
        executeCommand("docker", "rm", "-f", CONTAINER_NAME);

        // Start PostgreSQL container
        ProcessBuilder pb = new ProcessBuilder(
            "docker", "run", "-d",
            "--name", CONTAINER_NAME,
            "-e", "POSTGRES_DB=" + PG_DATABASE,
            "-e", "POSTGRES_USER=" + PG_USER,
            "-e", "POSTGRES_PASSWORD=" + PG_PASSWORD,
            "-p", PG_PORT + ":5432",
            "postgres:15-alpine"
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException(
                "Failed to start PostgreSQL container, exit code: " + exitCode);
        }

        // Wait for PostgreSQL to be ready
        Thread.sleep(3000);
        System.out.println("PostgreSQL container started: " + CONTAINER_NAME);
    }

    @AfterAll
    static void stopContainer() throws IOException, InterruptedException {
        executeCommand("docker", "rm", "-f", CONTAINER_NAME);
        System.out.println("PostgreSQL container removed: " + CONTAINER_NAME);
    }

    private static void executeCommand(String... command)
        throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();
    }

    private DatabaseClient createClient() {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, "postgresql")
            .option(ConnectionFactoryOptions.HOST, PG_HOST)
            .option(ConnectionFactoryOptions.PORT, PG_PORT)
            .option(ConnectionFactoryOptions.DATABASE, PG_DATABASE)
            .option(ConnectionFactoryOptions.USER, PG_USER)
            .option(ConnectionFactoryOptions.PASSWORD, PG_PASSWORD)
            .build();

        ConnectionFactory connectionFactory =
            ConnectionFactories.get(options);
        return DatabaseClient.create(connectionFactory);
    }

    @Test
    void shouldConnectToPostgresViaR2dbc() {
        DatabaseClient client = createClient();

        client.sql("SELECT 1 as result")
            .fetch().one()
            .map(row -> ((Number) row.get("result")).longValue())
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();

        System.out.println("R2DBC connection to PostgreSQL successful");
    }

    @Test
    void shouldCreateTableAndInsertData() {
        DatabaseClient client = createClient();

        // Drop table if exists
        client.sql("DROP TABLE IF EXISTS test_table")
            .fetch().rowsUpdated()
            .block();

        // Create table (DDL returns 0 in PostgreSQL)
        client.sql(
            "CREATE TABLE test_table (id BIGINT PRIMARY KEY, name VARCHAR(255))")
            .fetch().rowsUpdated()
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();

        // Insert data
        client.sql(
            "INSERT INTO test_table (id, name) VALUES (1, 'test')")
            .fetch().rowsUpdated()
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();

        // Query data
        client.sql("SELECT name FROM test_table WHERE id = 1")
            .fetch().one()
            .map(row -> row.get("name"))
            .as(StepVerifier::create)
            .expectNext("test")
            .verifyComplete();

        System.out.println("Table creation and data insertion successful");
    }

    @Test
    void shouldVerifyPostgresVersion() {
        DatabaseClient client = createClient();

        client.sql("SELECT version() as ver")
            .fetch().one()
            .map(row -> String.valueOf(row.get("ver")))
            .as(StepVerifier::create)
            .expectNextMatches(version -> version.contains("PostgreSQL"))
            .verifyComplete();

        System.out.println("PostgreSQL version check successful");
    }
}
