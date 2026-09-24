package run.ikaros.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import run.ikaros.integration.api.DurableEventConsumer;

@Testcontainers
@SpringBootTest(classes = OutboxPostgresDeliveryIntegrationTest.TestApplication.class)
class OutboxPostgresDeliveryIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> "r2dbc:postgresql://" + POSTGRES.getHost() + ":"
            + POSTGRES.getMappedPort(5432) + "/" + POSTGRES.getDatabaseName());
        registry.add("spring.r2dbc.username", POSTGRES::getUsername);
        registry.add("spring.r2dbc.password", POSTGRES::getPassword);
    }

    @Autowired private DatabaseClient database;
    @Autowired private OutboxEventRepository outbox;
    @Autowired private OutboxDeliveryRepository deliveries;
    @Autowired private DurableEventService events;

    @BeforeEach
    void createSchema() {
        execute("create table if not exists event_outbox (id uuid primary key, event_type varchar(256) not null, "
            + "schema_version integer not null, aggregate_type varchar(128) not null, aggregate_id uuid, "
            + "producer_subsystem varchar(128) not null, subject_type varchar(128) not null, subject_id uuid, "
            + "payload_json text not null, occurred_at timestamptz not null, attempt_count integer not null default 0, "
            + "last_attempt_at timestamptz, dispatched_at timestamptz, request_id varchar(128), "
            + "correlation_id varchar(128), causation_id varchar(128), actor_id uuid)");
        execute("create table if not exists event_inbox (id uuid primary key default gen_random_uuid(), "
            + "consumer_id varchar(256) not null, event_id uuid not null, processed_at timestamptz not null, "
            + "unique (consumer_id, event_id))");
        execute("create table if not exists event_delivery (id uuid primary key default gen_random_uuid(), "
            + "consumer_id varchar(256) not null, event_id uuid not null, status varchar(16) not null, "
            + "attempt_count integer not null default 0, next_attempt_at timestamptz not null, "
            + "last_attempt_at timestamptz, last_error_classification varchar(128), created_at timestamptz not null, "
            + "updated_at timestamptz not null, unique (consumer_id, event_id))");
        execute("create table if not exists consumer_effect (consumer_id varchar(256) not null, event_id uuid not null, "
            + "primary key (consumer_id, event_id))");
        execute("delete from consumer_effect");
        execute("delete from event_delivery");
        execute("delete from event_inbox");
        execute("delete from event_outbox");
    }

    @Test
    void oneConsumerFailureDoesNotHideEventFromAnotherConsumer() {
        UUID eventId = UUID.randomUUID();
        outbox.save(new OutboxEventEntity(eventId, "resource.resource.created", 1, "resource", eventId,
            "resource", "resource", eventId, "{}", java.time.Instant.now(), 0, null, null,
            null, null, null, null)).block();
        DurableEventConsumer search = consumer("search");

        assertEquals(1L, events.dispatchOnce(search).block());
        assertNotNull(outbox.findById(eventId).block().dispatchedAt());

        assertEquals(0L, events.dispatchOnce("notification", ignored ->
            database.sql("insert into consumer_effect (consumer_id, event_id) values ('notification', :eventId)")
                .bind("eventId", eventId).fetch().rowsUpdated()
                .then(Mono.<Void>error(new IllegalStateException("notification unavailable")))).block());

        OutboxDeliveryEntity failed = deliveries.lockByConsumerIdAndEventId("notification", eventId).block();
        assertEquals("RETRY", failed.status());
        assertEquals(1, failed.attemptCount());
        assertTrue(countEffects("search") == 1L);
        assertEquals(0L, countEffects("notification"));
        assertEquals(0L, events.dispatchOnce(search).block());
        assertEquals(eventId, events.pendingEvents("notification").map(event -> event.eventId()).blockFirst());
        assertTrue(events.pendingEvents("search").collectList().block().isEmpty());
        assertEquals(1L, outbox.countPending().block());
    }

    @Test
    void failedConsumerDeliveryMovesToDeadAfterConfiguredAttemptLimit() {
        UUID eventId = UUID.randomUUID();
        outbox.save(new OutboxEventEntity(eventId, "resource.resource.created", 1, "resource", eventId,
            "resource", "resource", eventId, "{}", java.time.Instant.now(), 0, null, null,
            null, null, null, null)).block();
        DurableEventConsumer failing = new DurableEventConsumer() {
            @Override public String consumerId() { return "poison-consumer"; }
            @Override public Mono<Void> consume(run.ikaros.integration.api.DurableEvent event) {
                return Mono.error(new IllegalStateException("consumer unavailable"));
            }
        };

        for (int attempt = 0; attempt < 8; attempt++) {
            assertEquals(0L, events.dispatchOnce(eventId, failing).block());
        }

        OutboxDeliveryEntity dead = deliveries.lockByConsumerIdAndEventId("poison-consumer", eventId).block();
        assertEquals("DEAD", dead.status());
        assertEquals(8, dead.attemptCount());
        assertEquals(eventId, events.pendingEvents("poison-consumer").map(event -> event.eventId()).blockFirst());
    }

    private DurableEventConsumer consumer(String id) {
        return new DurableEventConsumer() {
            @Override public String consumerId() { return id; }
            @Override public Mono<Void> consume(run.ikaros.integration.api.DurableEvent event) {
                return database.sql("insert into consumer_effect (consumer_id, event_id) values (:consumerId, :eventId)")
                    .bind("consumerId", id).bind("eventId", event.eventId()).fetch().rowsUpdated().then();
            }
        };
    }

    private long countEffects(String consumerId) {
        return database.sql("select count(*) as value from consumer_effect where consumer_id = :consumerId")
            .bind("consumerId", consumerId)
            .map((row, metadata) -> row.get("value", Long.class)).one().block();
    }

    private void execute(String sql) {
        database.sql(sql).fetch().rowsUpdated().block();
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableR2dbcRepositories(basePackageClasses = OutboxEventRepository.class)
    static class TestApplication {
        @Bean
        TransactionalOperator transactionOperator(ReactiveTransactionManager manager) {
            return TransactionalOperator.create(manager);
        }

        @Bean
        DurableEventService durableEventService(OutboxEventRepository outbox, InboxEntryRepository inbox,
            OutboxDeliveryRepository deliveries, TransactionalOperator transaction) {
            return new DurableEventService(outbox, inbox, deliveries, transaction);
        }
    }
}
