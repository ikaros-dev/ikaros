package run.ikaros.operations.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Duration;
import java.util.Map;
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
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;
import run.ikaros.common.ConflictException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.integration.api.EventAppendRequest;
import run.ikaros.integration.api.EventReference;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.TaskStatus;

@Testcontainers
@SpringBootTest(classes = PersistentBackgroundTaskPostgresIntegrationTest.TestApplication.class)
class PersistentBackgroundTaskPostgresIntegrationTest {
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
    @Autowired private BackgroundTaskRepository tasks;
    @Autowired private BackgroundTaskAttemptRepository attempts;
    @Autowired private PersistentBackgroundTaskService service;
    @Autowired private ControlledEventPublisher events;

    @BeforeEach
    void createSchema() {
        execute("create table if not exists background_task (id uuid primary key default gen_random_uuid(), "
            + "task_type varchar(256) not null, status varchar(32) not null, payload jsonb not null, "
            + "idempotency_key varchar(512), available_at timestamptz not null, timeout_at timestamptz, "
            + "lease_owner varchar(256), lease_token uuid, lease_expires_at timestamptz, attempt integer not null default 0, "
            + "cancel_requested_at timestamptz, progress jsonb not null default '{}'::jsonb, result_summary jsonb not null default '{}'::jsonb, "
            + "created_at timestamptz not null, updated_at timestamptz not null, parent_task_id uuid, version bigint not null default 0)");
        execute("create table if not exists background_task_attempt (id uuid primary key default gen_random_uuid(), "
            + "task_id uuid not null, attempt_no integer not null, status varchar(32) not null, claimed_by varchar(256), "
            + "lease_expires_at timestamptz, last_heartbeat_at timestamptz, started_at timestamptz, ended_at timestamptz, "
            + "error_summary text, created_at timestamptz not null, unique (task_id, attempt_no))");
        execute("delete from background_task_attempt");
        execute("delete from background_task");
        events.failOnEventType(null);
    }

    @Test
    void taskAndAttemptRollbackWhenStartedEventCannotBeAppended() {
        BackgroundTask pending = service.submit("test", Map.of(), "atomic-start").block();
        events.failOnEventType("operations.background-task.started");

        StepVerifier.create(service.claim("runner", Duration.ofMinutes(1)))
            .expectError(IllegalStateException.class)
            .verify();

        assertEquals(TaskStatus.PENDING, service.get(pending.id()).block().status());
        assertEquals(0L, attempts.findAllByTaskIdOrderByAttemptNoAsc(pending.id()).count().block());
    }

    @Test
    void previousLeaseCannotCompleteAfterTaskIsReclaimed() throws InterruptedException {
        BackgroundTask submitted = service.submit("long-task", Map.of(), "lease-reclaim").block();
        BackgroundTask first = service.claim("old-runner", Duration.ofMillis(100)).block();
        Thread.sleep(150);
        BackgroundTask second = service.claim("new-runner", Duration.ofMinutes(1)).block();

        assertEquals(submitted.id(), second.id());
        assertNotEquals(first.leaseToken(), second.leaseToken());
        StepVerifier.create(service.complete(first.id(), first.leaseToken(), Map.of("stale", true)))
            .expectError(ConflictException.class)
            .verify();
        assertEquals(TaskStatus.RUNNING, service.get(submitted.id()).block().status());
        assertEquals(TaskStatus.SUCCEEDED, service.complete(second.id(), second.leaseToken(), Map.of("ok", true))
            .block().status());
    }

    private void execute(String sql) {
        database.sql(sql).fetch().rowsUpdated().block();
    }

    static class ControlledEventPublisher implements DurableEventPublisher {
        private volatile String failEventType;

        void failOnEventType(String eventType) {
            failEventType = eventType;
        }

        @Override
        public Mono<EventReference> append(EventAppendRequest request) {
            if (request.eventType().equals(failEventType)) {
                return Mono.error(new IllegalStateException("simulated event append failure"));
            }
            return Mono.just(new EventReference(UUID.randomUUID(), request.eventType(), request.schemaVersion()));
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableR2dbcRepositories(basePackageClasses = BackgroundTaskRepository.class)
    static class TestApplication {
        @Bean
        TransactionalOperator transactionOperator(ReactiveTransactionManager manager) {
            return TransactionalOperator.create(manager);
        }

        @Bean ObjectMapper objectMapper() { return new ObjectMapper(); }

        @Bean ControlledEventPublisher controlledEventPublisher() { return new ControlledEventPublisher(); }

        @Bean
        PersistentBackgroundTaskService taskService(BackgroundTaskRepository tasks,
            BackgroundTaskAttemptRepository attempts, ObjectMapper mapper, DurableEventPublisher events,
            DatabaseClient database, TransactionalOperator transaction) {
            return new PersistentBackgroundTaskService(tasks, attempts, mapper, events, database, transaction);
        }
    }
}
