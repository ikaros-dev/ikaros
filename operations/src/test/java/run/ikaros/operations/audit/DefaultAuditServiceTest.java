package run.ikaros.operations.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.PrincipalContext;
import run.ikaros.operations.api.AuditActorType;
import run.ikaros.operations.api.AuditContext;
import run.ikaros.operations.api.AuditEventCommand;
import run.ikaros.operations.api.AuditResult;
import run.ikaros.operations.api.AuditRiskLevel;

/** 验证资源管理等操作写入独立审计事实，且不把审计与 Activity 混用。 */
class DefaultAuditServiceTest {
    @Test
    void persistsUserResourceManagementAuditEvent() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        DefaultAuditService service = new DefaultAuditService(repository);
        UUID actorId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();

        StepVerifier.create(service.record(actorId, "resource.update", "RESOURCE", resourceId, "{}"))
            .verifyComplete();

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(event -> {
            assertThat(event.actorType()).isEqualTo("USER");
            assertThat(event.actorId()).isEqualTo(actorId);
            assertThat(event.action()).isEqualTo("resource.update");
            assertThat(event.targetType()).isEqualTo("RESOURCE");
            assertThat(event.targetId()).isEqualTo(resourceId);
            assertThat(event.details()).isEqualTo("{}");
            assertThat(event.occurredAt()).isNotNull();
            return true;
        }));
    }

    @Test
    void recordsSystemResourceLifecycleOperationWithoutUserActivity() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        DefaultAuditService service = new DefaultAuditService(repository);
        UUID resourceId = UUID.randomUUID();

        StepVerifier.create(service.record(null, "resource.archive", "RESOURCE", resourceId, "{}"))
            .verifyComplete();

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(event ->
            "SYSTEM".equals(event.actorType()) && event.actorId() == null
                && "resource.archive".equals(event.action())));
    }

    @Test
    void redactsSensitiveDetailsBeforePersistence() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        DefaultAuditService service = new DefaultAuditService(repository);

        StepVerifier.create(service.record(UUID.randomUUID(), "security.change", "USER", UUID.randomUUID(),
            "{\"token\":\"jwt-secret\",\"password\":\"plain-password\",\"result\":\"ok\"}"))
            .verifyComplete();

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(event ->
            event.details().contains("[REDACTED]")
                && !event.details().contains("jwt-secret")
                && !event.details().contains("plain-password")
                && event.details().contains("\"result\":\"ok\"")));
    }

    @Test
    void persistsExplicitAdminResultRiskAndContext() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        DefaultAuditService service = new DefaultAuditService(repository);
        UUID actorId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();

        StepVerifier.create(service.record(new AuditEventCommand(
            AuditActorType.ADMIN, actorId, "identity.user.disable", "USER", targetId,
            AuditResult.DENIED, AuditRiskLevel.HIGH,
            "{\"step_up\":{\"verificationGrant\":\"grant-secret\"},\"reason\":\"policy\"}", 2,
            new AuditContext("request-1", "correlation-1")
        ))).verifyComplete();

        verify(repository).save(org.mockito.ArgumentMatchers.argThat(event ->
            "ADMIN".equals(event.actorType())
                && actorId.equals(event.actorId())
                && "DENIED".equals(event.result())
                && "HIGH".equals(event.riskLevel())
                && event.detailsSchemaVersion() == 2
                && "request-1".equals(event.requestId())
                && "correlation-1".equals(event.correlationId())
                && event.details().contains("[REDACTED]")
                && !event.details().contains("grant-secret")));
    }

    @Test
    void rejectsUnknownOrInvalidNewAuditEvents() {
        DefaultAuditService service = new DefaultAuditService(mock(AuditEventRepository.class));
        AuditEventCommand invalid = new AuditEventCommand(
            AuditActorType.USER, UUID.randomUUID(), "Resource.Update", "resource", null,
            AuditResult.UNKNOWN, AuditRiskLevel.NORMAL, "[]", 0, null
        );

        StepVerifier.create(service.record(invalid))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void recordsExactlyOnceAndUsesCurrentRequestContext() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        DefaultAuditService service = new DefaultAuditService(repository);
        UUID actorId = UUID.randomUUID();
        PrincipalContext context = new PrincipalContext(actorId, UUID.randomUUID(), "request-2", "correlation-2", false);

        StepVerifier.create(service.record(actorId, "resource.update", "RESOURCE", UUID.randomUUID(), "{}")
                .contextWrite(reactor.util.context.Context.of(PrincipalContext.CONTEXT_KEY, context)))
            .verifyComplete();

        verify(repository, times(1)).save(org.mockito.ArgumentMatchers.argThat(event ->
            "request-2".equals(event.requestId()) && "correlation-2".equals(event.correlationId())));
    }
}
