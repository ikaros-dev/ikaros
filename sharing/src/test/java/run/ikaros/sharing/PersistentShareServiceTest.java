package run.ikaros.sharing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
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
    void updatesExpiryOnlyForOwnedActiveShare() {
        UUID shareId = UUID.randomUUID();
        Instant createdAt = Instant.now().minusSeconds(10);
        ShareEntity existing = new ShareEntity(shareId, issuer, "RESOURCE", target,
                ShareGranteeType.LINK_TOKEN, null, "read", "digest", null,
                ShareStatus.ACTIVE, createdAt, createdAt, 0L);
        Instant expiresAt = Instant.now().plusSeconds(3600);
        when(repository.findById(shareId)).thenReturn(Mono.just(existing));
        when(repository.save(any(ShareEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.setExpiration(issuer, shareId, new SetShareExpirationRequest(expiresAt)))
                .assertNext(view -> assertThat(view.expiresAt()).isEqualTo(expiresAt))
                .verifyComplete();

        ArgumentCaptor<ShareEntity> saved = ArgumentCaptor.forClass(ShareEntity.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().expiresAt()).isEqualTo(expiresAt);
        assertThat(saved.getValue().tokenDigest()).isEqualTo(existing.tokenDigest());
    }

    @Test
    void rejectsPastExpiryBeforeReadingOrWriting() {
        StepVerifier.create(service.setExpiration(issuer, UUID.randomUUID(),
                        new SetShareExpirationRequest(Instant.now().minusSeconds(1))))
                .expectErrorMessage("Share 过期时间必须在未来")
                .verify();
        verify(repository, org.mockito.Mockito.never()).findById(org.mockito.ArgumentMatchers.any(UUID.class));
        verify(repository, org.mockito.Mockito.never()).save(any());
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

    @Test
    void reportsStableReasonForInvalidTokenWithoutEchoingSecret() {
        String token = "invalid-secret";
        when(repository.findByTokenDigest(digest(token))).thenReturn(Mono.empty());

        StepVerifier.create(service.redeem(token))
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(ShareAccessException.class);
                    ShareAccessException access = (ShareAccessException) error;
                    assertThat(access.reason()).isEqualTo(ShareAccessFailureReason.INVALID_TOKEN);
                    assertThat(access.code()).isEqualTo("share.access.invalid_token");
                    assertThat(access.getMessage()).doesNotContain(token);
                })
                .verify();
    }

    @Test
    void reportsRevokedAndExpiredReasons() {
        String revokedToken = "revoked-secret";
        String expiredToken = "expired-secret";
        when(repository.findByTokenDigest(digest(revokedToken))).thenReturn(Mono.just(entity(ShareStatus.REVOKED, null)));
        when(repository.findByTokenDigest(digest(expiredToken))).thenReturn(Mono.just(entity(ShareStatus.ACTIVE, Instant.now().minusSeconds(1))));

        StepVerifier.create(service.redeem(revokedToken))
                .expectErrorSatisfies(error -> assertThat(((ShareAccessException) error).reason())
                        .isEqualTo(ShareAccessFailureReason.REVOKED))
                .verify();
        StepVerifier.create(service.redeem(expiredToken))
                .expectErrorSatisfies(error -> assertThat(((ShareAccessException) error).reason())
                        .isEqualTo(ShareAccessFailureReason.EXPIRED))
                .verify();
    }

    @Test
    void reportsMissingTokenAsClientInputFailure() {
        StepVerifier.create(service.redeem(" "))
                .expectErrorSatisfies(error -> assertThat(((ShareAccessException) error).reason())
                        .isEqualTo(ShareAccessFailureReason.MISSING_TOKEN))
                .verify();
    }

    private ShareEntity entity(ShareStatus status, Instant expiresAt) {
        return new ShareEntity(UUID.randomUUID(), issuer, "RESOURCE", target, ShareGranteeType.LINK_TOKEN,
                null, "read", "digest", expiresAt, status, Instant.now().minusSeconds(10), Instant.now(), 0L);
    }

    private static String digest(String value) {
        try {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            throw new AssertionError(error);
        }
    }
}
