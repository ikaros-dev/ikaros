package run.ikaros.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.audit.AuditService;
import run.ikaros.common.ConflictException;

/**
 * 验证 Resource 聚合的关键业务规则。
 */
class DefaultResourceServiceTest {
    private ResourceRepository resourceRepository;
    private ResourceTitleRepository titleRepository;
    private ExternalIdentityRepository identityRepository;
    private AuditService auditService;
    private DefaultResourceService service;

    @BeforeEach
    void setUp() {
        resourceRepository = mock(ResourceRepository.class);
        titleRepository = mock(ResourceTitleRepository.class);
        identityRepository = mock(ExternalIdentityRepository.class);
        auditService = mock(AuditService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new DefaultResourceService(resourceRepository, titleRepository, identityRepository, auditService,
            transaction);
    }

    @Test
    void createsResourceWithPrimaryTitleAndAuditEvent() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceEntity saved = new ResourceEntity(resourceId, ownerId, ResourceType.BOOK,
            ResourceLifecycle.ACTIVE, now, now, null, 0L);
        ResourceTitleEntity title = new ResourceTitleEntity(UUID.randomUUID(), resourceId, "zh-CN", "测试书籍",
            true, now, now, 0L);

        when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(Mono.just(saved));
        when(titleRepository.save(any(ResourceTitleEntity.class))).thenReturn(Mono.just(title));
        when(titleRepository.findAllByResourceIdOrderByPrimaryDescLocaleAsc(resourceId)).thenReturn(Flux.just(title));
        when(identityRepository.findAllByResourceIdOrderByProviderAsc(resourceId)).thenReturn(Flux.empty());
        when(auditService.record(eq(ownerId), eq("resource.create"), eq("RESOURCE"), eq(resourceId), eq("{}")))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.create(ownerId, new CreateResourceRequest(ResourceType.BOOK, "测试书籍", "zh-CN")))
            .assertNext(view -> {
                assertThat(view.id()).isEqualTo(resourceId);
                assertThat(view.type()).isEqualTo(ResourceType.BOOK);
                assertThat(view.titles()).singleElement().satisfies(value -> {
                    assertThat(value.value()).isEqualTo("测试书籍");
                    assertThat(value.primary()).isTrue();
                });
            })
            .verifyComplete();

        ArgumentCaptor<ResourceEntity> resourceCaptor = ArgumentCaptor.forClass(ResourceEntity.class);
        verify(resourceRepository).save(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().id()).isNull();
        assertThat(resourceCaptor.getValue().ownerId()).isEqualTo(ownerId);
        assertThat(resourceCaptor.getValue().lifecycle()).isEqualTo(ResourceLifecycle.ACTIVE);
        verify(auditService).record(ownerId, "resource.create", "RESOURCE", resourceId, "{}");
    }

    @Test
    void movesResourceToTrashWithoutDeletingIt() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceEntity active = new ResourceEntity(resourceId, ownerId, ResourceType.VIDEO,
            ResourceLifecycle.ACTIVE, now, now, null, 2L);
        ResourceEntity trashed = new ResourceEntity(resourceId, ownerId, ResourceType.VIDEO,
            ResourceLifecycle.TRASHED, now, now, now, 3L);

        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(active));
        when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(Mono.just(trashed));
        when(auditService.record(ownerId, "resource.trash", "RESOURCE", resourceId, "{}"))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.trash(ownerId, resourceId)).verifyComplete();

        ArgumentCaptor<ResourceEntity> resourceCaptor = ArgumentCaptor.forClass(ResourceEntity.class);
        verify(resourceRepository).save(resourceCaptor.capture());
        assertThat(resourceCaptor.getValue().lifecycle()).isEqualTo(ResourceLifecycle.TRASHED);
        assertThat(resourceCaptor.getValue().deletedAt()).isNotNull();
        assertThat(resourceCaptor.getValue().version()).isEqualTo(2L);
    }

    @Test
    void reportsConflictWhenExternalIdentityIsAlreadyBound() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceEntity resource = new ResourceEntity(resourceId, ownerId, ResourceType.MUSIC,
            ResourceLifecycle.ACTIVE, now, now, null, 0L);

        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource));
        when(identityRepository.save(any(ExternalIdentityEntity.class)))
            .thenReturn(Mono.error(new DuplicateKeyException("duplicate")));

        StepVerifier.create(service.addExternalIdentity(ownerId, resourceId,
                new CreateExternalIdentityRequest("musicbrainz", "recording", "abc")))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ConflictException.class);
                assertThat(error).hasMessage("该外部身份已绑定到其他资源");
            })
            .verify();
    }
}
