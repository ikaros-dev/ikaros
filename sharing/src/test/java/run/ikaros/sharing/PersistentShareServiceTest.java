package run.ikaros.sharing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.CollectionOwnershipQuery;
import run.ikaros.resource.api.ResourceOwnershipQuery;

@ExtendWith(MockitoExtension.class)
class PersistentShareServiceTest {
    @Mock private ShareRepository repository;
    @Mock private ResourceOwnershipQuery resources;
    @Mock private CollectionOwnershipQuery collections;

    private PersistentShareService service;
    private UUID issuer;
    private UUID target;

    @BeforeEach
    void setUp() {
        service = new PersistentShareService(repository, resources, collections);
        issuer = UUID.randomUUID();
        target = UUID.randomUUID();
    }

    @Test
    void createsLinkOnlyAfterResourceOwnershipCheckAndReturnsTokenOnce() {
        when(resources.requireOwned(issuer, target)).thenReturn(Mono.empty());
        when(repository.save(any(ShareEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        ShareView view = service.create(issuer, new CreateShareRequest(
                "resource", target, ShareGranteeType.LINK_TOKEN, null, " read ", null)).block();

        assertThat(view).isNotNull();
        assertThat(view.targetType()).isEqualTo("RESOURCE");
        assertThat(view.capabilities()).isEqualTo("read");
        assertThat(view.token()).isNotBlank();
        ArgumentCaptor<ShareEntity> saved = ArgumentCaptor.forClass(ShareEntity.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().tokenDigest()).isNotEqualTo(view.token());
        assertThat(saved.getValue().status()).isEqualTo(ShareStatus.ACTIVE);
    }

    @Test
    void rejectsMissingTargetWithoutWritingShare() {
        when(resources.requireOwned(issuer, target)).thenReturn(Mono.error(new IllegalArgumentException("目标不存在")));

        StepVerifier.create(service.create(issuer, new CreateShareRequest(
                        "RESOURCE", target, ShareGranteeType.LINK_TOKEN, null, "read", null)))
                .expectErrorMessage("目标不存在")
                .verify();

        verify(repository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void supportsOwnedCollectionTargets() {
        when(collections.requireOwned(issuer, target)).thenReturn(Mono.empty());
        when(repository.save(any(ShareEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.create(issuer, new CreateShareRequest(
                        "COLLECTION", target, ShareGranteeType.USER, UUID.randomUUID(), "read", Instant.now().plusSeconds(60))))
                .assertNext(view -> assertThat(view.targetType()).isEqualTo("COLLECTION"))
                .verifyComplete();
    }

    @Test
    void rejectsUnsupportedTargetAndPastExpiryBeforeWriting() {
        StepVerifier.create(service.create(issuer, new CreateShareRequest(
                        "ATTACHMENT", target, ShareGranteeType.LINK_TOKEN, null, "read", null)))
                .expectErrorMessage("不支持的分享目标类型")
                .verify();
        StepVerifier.create(service.create(issuer, new CreateShareRequest(
                        "RESOURCE", target, ShareGranteeType.LINK_TOKEN, null, "read", Instant.now().minusSeconds(1))))
                .expectErrorMessage("Share 过期时间必须在未来")
                .verify();
        verify(repository, org.mockito.Mockito.never()).save(any());
    }
}
