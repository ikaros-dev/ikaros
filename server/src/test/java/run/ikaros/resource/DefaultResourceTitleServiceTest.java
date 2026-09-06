package run.ikaros.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.audit.AuditService;

/**
 * 验证 Resource 标题的语言唯一性、主标题切换和删除规则。
 */
class DefaultResourceTitleServiceTest {
    private ResourceRepository resourceRepository;
    private ResourceTitleRepository titleRepository;
    private AuditService auditService;
    private DefaultResourceTitleService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        titleRepository = mock(ResourceTitleRepository.class);
        auditService = mock(AuditService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DefaultResourceTitleService(resourceRepository, titleRepository, auditService, transaction);
    }

    @Test
    void setsNewPrimaryTitleAndDemotesPreviousPrimary() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceEntity resource = new ResourceEntity(resourceId, ownerId, ResourceType.BOOK, ResourceLifecycle.ACTIVE,
            now, now, null, 0L);
        ResourceTitleEntity oldTitle = new ResourceTitleEntity(UUID.randomUUID(), resourceId, "zh-CN", "旧标题", true,
            now, now, 0L);
        ResourceTitleEntity savedOld = new ResourceTitleEntity(oldTitle.id(), resourceId, "zh-CN", "旧标题", false,
            now, now, 1L);
        ResourceTitleEntity savedNew = new ResourceTitleEntity(UUID.randomUUID(), resourceId, "en", "New title", true,
            now, now, 0L);
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource));
        when(titleRepository.findAllByResourceIdOrderByPrimaryDescLocaleAsc(resourceId)).thenReturn(Flux.just(oldTitle));
        when(titleRepository.saveAll(any(Iterable.class))).thenReturn(Flux.just(savedOld, savedNew));
        when(auditService.record(ownerId, "resource.title.set", "RESOURCE", resourceId, "{}"))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.set(ownerId, resourceId, new SetResourceTitleRequest("en", "New title", true)))
            .assertNext(view -> {
                assertThat(view.value()).isEqualTo("New title");
                assertThat(view.primary()).isTrue();
            })
            .verifyComplete();

        verify(titleRepository).saveAll(any(Iterable.class));
    }

    @Test
    void refusesToDeleteLastTitle() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID titleId = UUID.randomUUID();
        Instant now = Instant.now();
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(
            new ResourceEntity(resourceId, ownerId, ResourceType.BOOK, ResourceLifecycle.ACTIVE, now, now, null, 0L)));
        when(titleRepository.findAllByResourceIdOrderByPrimaryDescLocaleAsc(resourceId)).thenReturn(Flux.just(
            new ResourceTitleEntity(titleId, resourceId, "zh-CN", "唯一标题", true, now, now, 0L)));

        StepVerifier.create(service.delete(ownerId, resourceId, titleId))
            .expectErrorMessage("Resource 至少需要保留一个标题")
            .verify();
    }

    @Test
    void promotesAnotherTitleWhenDeletingPrimary() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceTitleEntity primary = new ResourceTitleEntity(UUID.randomUUID(), resourceId, "zh-CN", "中文", true,
            now, now, 0L);
        ResourceTitleEntity secondary = new ResourceTitleEntity(UUID.randomUUID(), resourceId, "en", "English", false,
            now, now, 0L);
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(
            new ResourceEntity(resourceId, ownerId, ResourceType.BOOK, ResourceLifecycle.ACTIVE, now, now, null, 0L)));
        when(titleRepository.findAllByResourceIdOrderByPrimaryDescLocaleAsc(resourceId)).thenReturn(Flux.just(primary,
            secondary));
        when(titleRepository.save(any(ResourceTitleEntity.class))).thenReturn(Mono.just(secondary));
        when(titleRepository.deleteById(primary.id())).thenReturn(Mono.empty());
        when(auditService.record(ownerId, "resource.title.delete", "RESOURCE", resourceId, "{}"))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.delete(ownerId, resourceId, primary.id())).verifyComplete();

        verify(titleRepository).deleteById(primary.id());
    }
}
