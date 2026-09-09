package run.ikaros.operations.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

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
}
