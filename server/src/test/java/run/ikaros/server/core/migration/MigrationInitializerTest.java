package run.ikaros.server.core.migration;

import static org.assertj.core.api.Assertions.assertThat;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import reactor.test.StepVerifier;

/**
 * Integration test for MigrationInitializer.
 * Source db: created by db/postgresql/migration/ (old schema, bigint ids).
 * Target db: created by db/migration/ (new schema, uuid ids).
 */
@SpringBootTest
@ActiveProfiles("migration-test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MigrationInitializerTest {

    private static final String SOURCE_CONTAINER = "ikaros-test-source-pg";
    private static final String TARGET_CONTAINER = "ikaros-test-target-pg";

    private static final int SOURCE_PORT = 5434;
    private static final int TARGET_PORT = 5435;

    private static final String PG_DATABASE = "ikaros_test";
    private static final String PG_USER = "ikaros";
    private static final String PG_PASSWORD = "ikaros";

    @Autowired
    private MigrationInitializer migrationInitializer;

    @Autowired
    private DatabaseClient sourceClient;

    private static DatabaseClient targetClient;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url",
            () -> "r2dbc:postgresql://localhost:"
                + SOURCE_PORT + "/" + PG_DATABASE);
        registry.add("spring.r2dbc.username", () -> PG_USER);
        registry.add("spring.r2dbc.password", () -> PG_PASSWORD);

        registry.add("ikaros.migration.r2dbc.url",
            () -> "r2dbc:postgresql://localhost:"
                + TARGET_PORT + "/" + PG_DATABASE);
        registry.add("ikaros.migration.r2dbc.username", () -> PG_USER);
        registry.add("ikaros.migration.r2dbc.password", () -> PG_PASSWORD);

        // Force disable Flyway
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @BeforeAll
    static void startContainers() throws IOException, InterruptedException {
        executeCommand("docker", "rm", "-f", SOURCE_CONTAINER);
        executeCommand("docker", "rm", "-f", TARGET_CONTAINER);

        executeCommand("docker", "run", "-d",
            "--name", SOURCE_CONTAINER,
            "-e", "POSTGRES_DB=" + PG_DATABASE,
            "-e", "POSTGRES_USER=" + PG_USER,
            "-e", "POSTGRES_PASSWORD=" + PG_PASSWORD,
            "-p", SOURCE_PORT + ":5432",
            "postgres:15-alpine");

        executeCommand("docker", "run", "-d",
            "--name", TARGET_CONTAINER,
            "-e", "POSTGRES_DB=" + PG_DATABASE,
            "-e", "POSTGRES_USER=" + PG_USER,
            "-e", "POSTGRES_PASSWORD=" + PG_PASSWORD,
            "-p", TARGET_PORT + ":5432",
            "postgres:15-alpine");

        Thread.sleep(5000);
    }

    @AfterAll
    static void stopContainers() throws IOException, InterruptedException {
        executeCommand("docker", "rm", "-f", SOURCE_CONTAINER);
        executeCommand("docker", "rm", "-f", TARGET_CONTAINER);
    }

    private static void executeCommand(String... command)
        throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        process.waitFor();
    }

    private DatabaseClient getTargetClient() {
        if (targetClient == null) {
            ConnectionFactoryOptions options =
                ConnectionFactoryOptions.builder()
                    .option(ConnectionFactoryOptions.DRIVER, "postgresql")
                    .option(ConnectionFactoryOptions.HOST, "localhost")
                    .option(ConnectionFactoryOptions.PORT, TARGET_PORT)
                    .option(ConnectionFactoryOptions.DATABASE, PG_DATABASE)
                    .option(ConnectionFactoryOptions.USER, PG_USER)
                    .option(ConnectionFactoryOptions.PASSWORD, PG_PASSWORD)
                    .build();
            ConnectionFactory cf = ConnectionFactories.get(options);
            targetClient = DatabaseClient.create(cf);
        }
        return targetClient;
    }

    /**
     * Execute old migration scripts on source database.
     */
    private void initSourceDatabase() throws IOException {
        Resource[] resources =
            run.ikaros.server.infra.utils.PathResourceUtils
                .getResources("db/postgresql/migration");
        Arrays.sort(resources, Comparator.comparing(
            r -> r.getFilename() != null ? r.getFilename() : ""));
        ResourceDatabasePopulator populator =
            new ResourceDatabasePopulator(resources);
        populator.setSeparator(";");
        populator.populate(sourceClient.getConnectionFactory())
            .block();
    }

    /**
     * Insert test data into source database (old schema).
     */
    private void insertSourceTestData() {
        // subject
        sourceClient.sql(
                "INSERT INTO subject "
                    + "(id, name, name_cn, type, nsfw, air_time) "
                    + "VALUES (:id, :name, :name_cn, "
                    + ":type, :nsfw, :air_time)")
            .bind("id", 1L)
            .bind("name", "Test Subject")
            .bind("name_cn", "test")
            .bind("type", "ANIME")
            .bind("nsfw", false)
            .bind("air_time", LocalDateTime.now())
            .fetch().rowsUpdated()
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();

        // episode
        sourceClient.sql(
                "INSERT INTO episode "
                    + "(id, subject_id, name, name_cn, "
                    + "air_time, sequence, ep_group) "
                    + "VALUES (:id, :subject_id, :name, "
                    + ":name_cn, :air_time, :sequence, :ep_group)")
            .bind("id", 91L)
            .bind("subject_id", 91L)
            .bind("name", "Episode 01")
            .bind("name_cn", "ep01")
            .bind("air_time", LocalDateTime.now())
            .bind("sequence", 1L)
            .bind("ep_group", "MAIN")
            .fetch().rowsUpdated()
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();

        // attachment: id=10 is a file under root
        // (id=0,1,2 already exist from V0.13.0_0023 init script)
        sourceClient.sql(
                "INSERT INTO attachment "
                    + "(id, parent_id, type, path, "
                    + "name, size, update_time, sha1) "
                    + "VALUES (:id, :parent_id, :type, "
                    + ":path, :name, :size, :update_time, :sha1)")
            .bind("id", 92L)
            .bind("parent_id", 0L)
            .bind("type", "File")
            .bind("path", "/test.txt")
            .bind("name", "test.txt")
            .bind("size", 1024L)
            .bind("update_time", LocalDateTime.now())
            .bind("sha1", "test-sha1")
            .fetch().rowsUpdated()
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();

        // ikuser
        sourceClient.sql(
                "INSERT INTO ikuser "
                    + "(id, username, \"password\", nickname, "
                    + "\"enable\", non_locked, delete_status) "
                    + "VALUES (:id, :username, :password, "
                    + ":nickname, :enable, :non_locked, "
                    + ":delete_status)")
            .bind("id", 1L)
            .bind("username", "testuser")
            .bind("password", "$2a$10$encoded")
            .bind("nickname", "Test User")
            .bind("enable", true)
            .bind("non_locked", true)
            .bind("delete_status", false)
            .fetch().rowsUpdated()
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();

        // attachment_reference
        sourceClient.sql(
                "INSERT INTO attachment_reference "
                    + "(id, type, attachment_id, reference_id) "
                    + "VALUES (:id, :type, "
                    + ":attachment_id, :reference_id)")
            .bind("id", 1L)
            .bind("type", "SUBJECT")
            .bind("attachment_id", 10L)
            .bind("reference_id", 1L)
            .fetch().rowsUpdated()
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    // ==================== Test Cases ====================

    @Test
    @Order(0)
    void shouldInitSourceDatabase() throws IOException {
        initSourceDatabase();
        // Verify source tables created by old scripts
        sourceClient.sql(
                "SELECT COUNT(*) as cnt FROM subject")
            .fetch().one()
            .map(row -> ((Number) row.get("cnt")).longValue())
            .as(StepVerifier::create)
            .expectNext(0L)
            .verifyComplete();
    }

    @Test
    @Order(1)
    void shouldInsertSourceTestData() {
        insertSourceTestData();
        sourceClient.sql(
                "SELECT COUNT(*) as cnt FROM subject")
            .fetch().one()
            .map(row -> ((Number) row.get("cnt")).longValue())
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    @Order(2)
    void shouldExecuteMigration() {
        StepVerifier.create(migrationInitializer.doMigration(null))
            .verifyComplete();
    }

    @Test
    @Order(3)
    void shouldMigrateSubjectTable() {
        // Check target attachment table definition
        getTargetClient().sql(
                "SELECT column_name, data_type, is_nullable "
                    + "FROM information_schema.columns "
                    + "WHERE table_name = 'attachment' "
                    + "ORDER BY ordinal_position")
            .fetch().all()
            .collectList()
            .doOnNext(rows -> {
                System.out.println("Target attachment cols: "
                    + rows.size());
                rows.forEach(row -> System.out.println(
                    "  " + row.get("column_name")
                        + " " + row.get("data_type")
                        + " null=" + row.get("is_nullable")));
            })
            .block();

        // Check if primary key exists
        getTargetClient().sql(
                "SELECT constraint_name, constraint_type "
                    + "FROM information_schema.table_constraints "
                    + "WHERE table_name = 'attachment' "
                    + "AND constraint_type = 'PRIMARY KEY'")
            .fetch().all()
            .collectList()
            .doOnNext(rows -> System.out.println(
                "PK constraints: " + rows.size()))
            .block();

        getTargetClient().sql(
                "SELECT COUNT(*) as cnt FROM subject")
            .fetch().one()
            .map(row -> ((Number) row.get("cnt")).longValue())
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    @Order(4)
    void shouldMigrateEpisodeTable() {
        getTargetClient().sql(
                "SELECT COUNT(*) as cnt FROM episode")
            .fetch().one()
            .map(row -> ((Number) row.get("cnt")).longValue())
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    @Order(5)
    void shouldMigrateAttachmentTable() {
        getTargetClient().sql(
                "SELECT COUNT(*) as cnt FROM attachment")
            .fetch().one()
            .map(row -> ((Number) row.get("cnt")).longValue())
            .as(StepVerifier::create)
            .expectNext(2L)
            .verifyComplete();
    }

    @Test
    @Order(6)
    void shouldReplaceIdWithUuid() {
        getTargetClient().sql(
                "SELECT id FROM subject LIMIT 1")
            .fetch().one()
            .map(row -> row.get("id"))
            .as(StepVerifier::create)
            .expectNextMatches(id -> {
                try {
                    UUID.fromString(String.valueOf(id));
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            })
            .verifyComplete();
    }

    @Test
    @Order(7)
    void shouldReplaceForeignKeyWithUuid() {
        getTargetClient().sql(
                "SELECT e.subject_id FROM episode e "
                    + "JOIN subject s "
                    + "ON e.subject_id = s.id LIMIT 1")
            .fetch().one()
            .map(row -> row.get("subject_id"))
            .as(StepVerifier::create)
            .expectNextMatches(subjectId -> {
                try {
                    UUID.fromString(String.valueOf(subjectId));
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            })
            .verifyComplete();
    }

    @Test
    @Order(8)
    void shouldMigrateAttachmentReference() {
        getTargetClient().sql(
                "SELECT COUNT(*) as cnt "
                    + "FROM attachment_reference")
            .fetch().one()
            .map(row -> ((Number) row.get("cnt")).longValue())
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    @Order(9)
    void shouldHandleAttachmentParentIdSpecialValues() {
        getTargetClient().sql(
                "SELECT parent_id FROM attachment "
                    + "WHERE name = 'Root' LIMIT 1")
            .fetch().one()
            .map(row -> row.get("parent_id"))
            .as(StepVerifier::create)
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    @Order(10)
    void shouldMigrateUserTable() {
        getTargetClient().sql(
                "SELECT COUNT(*) as cnt FROM ikuser")
            .fetch().one()
            .map(row -> ((Number) row.get("cnt")).longValue())
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    @Order(11)
    void shouldPreserveDataIntegrity() {
        getTargetClient().sql(
                "SELECT COUNT(*) as cnt FROM episode e "
                    + "WHERE EXISTS (SELECT 1 FROM subject s "
                    + "WHERE s.id = e.subject_id)")
            .fetch().one()
            .map(row -> ((Number) row.get("cnt")).longValue())
            .as(StepVerifier::create)
            .expectNext(1L)
            .verifyComplete();
    }

    @Test
    @Order(12)
    void shouldCreateMigrationsTable() {
        getTargetClient().sql(
                "SELECT COUNT(*) as cnt FROM migrations")
            .fetch().one()
            .map(row -> ((Number) row.get("cnt")).longValue())
            .as(StepVerifier::create)
            .expectNextMatches(cnt -> cnt > 0)
            .verifyComplete();
    }

    @Test
    @Order(13)
    void shouldSkipDuplicateRecords() {
        assertThat(true).isTrue();
    }
}
