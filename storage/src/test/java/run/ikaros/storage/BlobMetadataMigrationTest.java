package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.r2dbc.spi.ConnectionFactories;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.DefaultUuidV7Generator;

/** Executes the actual metadata Migration against PostgreSQL, without a Spring context. */
@Testcontainers
class BlobMetadataMigrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    private static DatabaseClient database;
    private UUID blobId;

    @BeforeAll
    static void migrate() {
        var factory = ConnectionFactories.get("r2dbc:postgresql://" + POSTGRES.getUsername() + ":"
            + POSTGRES.getPassword() + "@" + POSTGRES.getHost() + ":" + POSTGRES.getMappedPort(5432)
            + "/" + POSTGRES.getDatabaseName());
        database = DatabaseClient.create(factory);
        new ResourceDatabasePopulator(
            new ClassPathResource("db/migration/V202609020900__DDL_FOUNDATION_UUID.sql"),
            new ClassPathResource("db/migration/V202609021050__DDL_STORAGE_CORE.sql"),
            new ClassPathResource("db/migration/V202610030048__DDL_STORAGE_BLOB_METADATA.sql")
        ).populate(factory).block();
    }

    @BeforeEach
    void createBlob() {
        blobId = createBlobRow();
    }

    private UUID createBlobRow() {
        String hash = new DefaultUuidV7Generator().next().toString().replace("-", "").repeat(2);
        return database.sql("insert into blob (sha256, size_bytes) values (:hash, 10) returning id")
            .bind("hash", hash).map((row, metadata) -> row.get("id", UUID.class)).one().block();
    }

    @Test
    void acceptsJsonTypesAndGeneratesUuidV7TimestampAndVersionDefaults() {
        Map<String, String> values = Map.of("number", "120.5", "string", "\"h264\"",
            "array", "[\"aac\", \"flac\"]", "object", "{\"width\":1920}");
        values.forEach((type, json) -> StepVerifier.create(database.sql("""
                insert into blob_metadata (blob_id, field_key, field_value)
                values (:blobId, :key, cast(:value as jsonb))
                returning id, updated_at, version, jsonb_typeof(field_value) as value_type
                """)
            .bind("blobId", blobId).bind("key", type).bind("value", json)
            .map((row, metadata) -> {
                assertEquals(7, row.get("id", UUID.class).version());
                assertNotNull(row.get("updated_at", OffsetDateTime.class));
                assertEquals(0L, row.get("version", Long.class));
                return row.get("value_type", String.class);
            }).one()).expectNext(type).verifyComplete());
    }

    @Test
    void enforcesUniquenessWithinBlobAndAllowsSameKeyOnAnotherBlob() {
        StepVerifier.create(insert(blobId, "duration", "120")).expectNext(1L).verifyComplete();
        StepVerifier.create(insert(blobId, "duration", "121"))
            .expectError(DataIntegrityViolationException.class).verify();
        StepVerifier.create(insert(createBlobRow(), "duration", "121"))
            .expectNext(1L).verifyComplete();
    }

    @Test
    void rejectsUnknownBlobInvalidKeysNullValueAndNegativeVersion() {
        StepVerifier.create(insert(new DefaultUuidV7Generator().next(), "duration", "120"))
            .expectError(DataIntegrityViolationException.class).verify();
        for (String key : new String[] {"", " ", " duration", "duration "}) {
            StepVerifier.create(insert(blobId, key, "120"))
                .expectError(DataIntegrityViolationException.class).verify();
        }
        StepVerifier.create(database.sql("""
            insert into blob_metadata (blob_id, field_key, field_value)
            values (:blobId, 'duration', null)
            """).bind("blobId", blobId).fetch().rowsUpdated())
            .expectError(DataIntegrityViolationException.class).verify();
        StepVerifier.create(database.sql("""
            insert into blob_metadata (blob_id, field_key, field_value, version)
            values (:blobId, 'duration', '120'::jsonb, -1)
            """).bind("blobId", blobId).fetch().rowsUpdated())
            .expectError(DataIntegrityViolationException.class).verify();
    }

    @Test
    void deletingBlobCleansItsMetadataAndPreservesOtherBlobMetadata() {
        UUID otherBlob = createBlobRow();
        StepVerifier.create(insert(blobId, "duration", "120").then(insert(otherBlob, "duration", "121")))
            .expectNext(1L).verifyComplete();
        StepVerifier.create(database.sql("delete from blob where id = :id")
            .bind("id", blobId).fetch().rowsUpdated()).expectNext(1L).verifyComplete();
        StepVerifier.create(database.sql("select blob_id from blob_metadata where blob_id in (:id, :other)")
            .bind("id", blobId).bind("other", otherBlob)
            .map((row, metadata) -> row.get("blob_id", UUID.class)).all())
            .expectNext(otherBlob).verifyComplete();
    }

    private Mono<Long> insert(UUID id, String key, String value) {
        return database.sql("""
            insert into blob_metadata (blob_id, field_key, field_value)
            values (:blobId, :key, cast(:value as jsonb))
            """).bind("blobId", id).bind("key", key).bind("value", value).fetch().rowsUpdated();
    }
}
