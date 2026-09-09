package run.ikaros.operations.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** 验证审计按操作者/时间查询的稳定排序、分页和参数边界。 */
class AuditQueryServiceTest {
    @Test
    void returnsFilteredPageWithStableRepositoryQuery() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        AuditQueryService service = new AuditQueryService(repository);
        UUID actorId = UUID.randomUUID();
        Instant from = Instant.parse("2026-09-01T00:00:00Z");
        Instant to = Instant.parse("2026-09-10T00:00:00Z");
        AuditEventEntity event = new AuditEventEntity(UUID.randomUUID(), "USER", actorId, "resource.update",
            "RESOURCE", UUID.randomUUID(), "{}", Instant.parse("2026-09-05T00:00:00Z"), 0L);
        when(repository.search(actorId, from, to, 20, 40)).thenReturn(Flux.just(event));
        when(repository.countSearch(actorId, from, to)).thenReturn(Mono.just(41L));

        StepVerifier.create(service.search(actorId, from, to, 2, 20))
            .assertNext(page -> {
                assertThat(page.items()).containsExactly(event);
                assertThat(page.total()).isEqualTo(41L);
                assertThat(page.page()).isEqualTo(2);
            }).verifyComplete();
        verify(repository).search(actorId, from, to, 20, 40);
    }

    @Test
    void rejectsInvalidTimeRangeAndPageSize() {
        AuditQueryService service = new AuditQueryService(mock(AuditEventRepository.class));
        Instant now = Instant.now();

        StepVerifier.create(service.search(null, now, now, 0, 20))
            .expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(service.search(null, null, null, 0, 101))
            .expectError(IllegalArgumentException.class).verify();
    }

    @Test
    void returnsRelatedAuditEventOrNotFound() {
        AuditEventRepository repository = mock(AuditEventRepository.class);
        AuditQueryService service = new AuditQueryService(repository);
        UUID eventId = UUID.randomUUID();
        AuditEventEntity event = new AuditEventEntity(eventId, "USER", UUID.randomUUID(), "resource.update",
            "RESOURCE", UUID.randomUUID(), "{}", Instant.now(), 0L);
        when(repository.findById(eventId)).thenReturn(Mono.just(event));
        StepVerifier.create(service.get(eventId)).expectNext(event).verifyComplete();

        UUID missingId = UUID.randomUUID();
        when(repository.findById(missingId)).thenReturn(Mono.empty());
        StepVerifier.create(service.get(missingId)).expectError(run.ikaros.common.NotFoundException.class).verify();
    }
}
