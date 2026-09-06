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
import run.ikaros.event.DurableEventService;

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

    @Test
    void rejectsInvalidListPagingBeforeRepositoryQuery() {
        StepVerifier.create(service.list(UUID.randomUUID(), null, null, -1, 20))
            .expectError(IllegalArgumentException.class).verify();
        StepVerifier.create(service.list(UUID.randomUUID(), null, null, 0, 101))
            .expectError(IllegalArgumentException.class).verify();
        org.mockito.Mockito.verifyNoInteractions(resourceRepository);
    }

    @Test
    void publishesExternalIdentityLifecycleEventsWithCanonicalProviderFields() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID identityId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceEntity resource = new ResourceEntity(resourceId, ownerId, ResourceType.MUSIC,
            ResourceLifecycle.ACTIVE, now, now, null, 0L);
        ExternalIdentityEntity identity = new ExternalIdentityEntity(identityId, resourceId,
            "musicbrainz:subject", "recording", "abc", now, now, 0L);
        DurableEventService events = mock(DurableEventService.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(resource));
        when(identityRepository.save(any(ExternalIdentityEntity.class))).thenReturn(Mono.just(identity));
        when(identityRepository.findByIdAndResourceId(identityId, resourceId)).thenReturn(Mono.just(identity));
        when(identityRepository.deleteById(identityId)).thenReturn(Mono.empty());
        when(auditService.record(eq(ownerId), eq("resource.external-identity.create"), eq("RESOURCE"),
            eq(resourceId), eq("{}"))).thenReturn(Mono.empty());
        when(auditService.record(eq(ownerId), eq("resource.external-identity.delete"), eq("RESOURCE"),
            eq(resourceId), eq("{}"))).thenReturn(Mono.empty());
        when(events.append(any(), eq(1), eq("resource"), eq(resourceId), any())).thenReturn(Mono.empty());
        DefaultResourceService eventService = new DefaultResourceService(resourceRepository, titleRepository,
            identityRepository, auditService, transaction, events);

        StepVerifier.create(eventService.addExternalIdentity(ownerId, resourceId,
                new CreateExternalIdentityRequest("musicbrainz:subject", "recording", "abc")))
            .expectNextCount(1).verifyComplete();
        StepVerifier.create(eventService.detachExternalIdentity(ownerId, resourceId, identityId))
            .verifyComplete();

        verify(events).append(eq("resource.external-identity.attached"), eq(1), eq("resource"),
            eq(resourceId), eq("{\"resource_id\":\"" + resourceId
                + "\",\"provider\":\"musicbrainz\",\"namespace\":\"subject\","
                + "\"object_type\":\"recording\",\"external_id\":\"abc\"}"));
        verify(events).append(eq("resource.external-identity.detached"), eq(1), eq("resource"),
            eq(resourceId), eq("{\"resource_id\":\"" + resourceId
                + "\",\"provider\":\"musicbrainz\",\"namespace\":\"subject\","
                + "\"object_type\":\"recording\",\"external_id\":\"abc\"}"));
    }

    @Test
    void replaysSameResourceForSameIdempotencyKeyAndRejectsDifferentPayload() {
        UUID ownerId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        Instant now = Instant.now();
        ResourceEntity saved = new ResourceEntity(resourceId, ownerId, ResourceType.BOOK,
            "测试书籍", null, ResourceClassification.PRIVATE, ResourceLifecycle.ACTIVE, now, now, null, 0L);
        CreateResourceRequest request = new CreateResourceRequest(ResourceType.BOOK, "测试书籍", "zh-CN");
        ResourceCreationIdempotencyRepository idempotency = mock(ResourceCreationIdempotencyRepository.class);
        TransactionalOperator transaction = mock(TransactionalOperator.class);
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(resourceRepository.save(any(ResourceEntity.class))).thenReturn(Mono.just(saved));
        when(titleRepository.save(any(ResourceTitleEntity.class))).thenReturn(Mono.just(
            new ResourceTitleEntity(UUID.randomUUID(), resourceId, "zh-CN", "测试书籍", true, now, now, 0L)));
        when(titleRepository.findAllByResourceIdOrderByPrimaryDescLocaleAsc(resourceId)).thenReturn(Flux.empty());
        when(identityRepository.findAllByResourceIdOrderByProviderAsc(resourceId)).thenReturn(Flux.empty());
        when(auditService.record(any(), eq("resource.create"), eq("RESOURCE"), eq(resourceId), eq("{}")))
            .thenReturn(Mono.empty());
        when(idempotency.save(any(ResourceCreationIdempotencyEntity.class))).thenAnswer(invocation ->
            Mono.just(invocation.getArgument(0)));
        when(idempotency.findByOwnerIdAndIdempotencyKey(ownerId, "create-1")).thenReturn(Mono.empty());
        DefaultResourceService eventService = new DefaultResourceService(resourceRepository, titleRepository,
            identityRepository, auditService, transaction, null, idempotency);

        StepVerifier.create(eventService.create(ownerId, request, "create-1"))
            .expectNextCount(1).verifyComplete();
        ArgumentCaptor<ResourceCreationIdempotencyEntity> capture =
            ArgumentCaptor.forClass(ResourceCreationIdempotencyEntity.class);
        verify(idempotency).save(capture.capture());
        ResourceCreationIdempotencyEntity record = capture.getValue();
        when(idempotency.findByOwnerIdAndIdempotencyKey(ownerId, "create-1")).thenReturn(Mono.just(record));
        when(resourceRepository.findByIdAndOwnerId(resourceId, ownerId)).thenReturn(Mono.just(saved));

        StepVerifier.create(eventService.create(ownerId, request, "create-1"))
            .expectNextCount(1).verifyComplete();
        StepVerifier.create(eventService.create(ownerId,
                new CreateResourceRequest(ResourceType.BOOK, "另一标题", "zh-CN"), "create-1"))
            .expectErrorSatisfies(error -> assertThat(error).isInstanceOf(ConflictException.class)
                .hasMessage("idempotency.key_reused")).verify();
    }
}
