package run.ikaros.sharing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.CollectionOwnershipQuery;
import run.ikaros.resource.api.ResourceOwnershipQuery;

@ExtendWith(MockitoExtension.class)
class ShareRevokeServiceTest {
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
  void revokesOwnedActiveShareAndPersistsRevokedStatus() {
    UUID shareId = UUID.randomUUID();
    ShareEntity existing = entity(shareId, issuer, ShareStatus.ACTIVE);
    when(repository.findById(shareId)).thenReturn(Mono.just(existing));
    when(repository.save(any(ShareEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(service.revoke(issuer, shareId))
        .assertNext(view -> assertThat(view.status()).isEqualTo(ShareStatus.REVOKED))
        .verifyComplete();

    ArgumentCaptor<ShareEntity> saved = ArgumentCaptor.forClass(ShareEntity.class);
    verify(repository).save(saved.capture());
    assertThat(saved.getValue().status()).isEqualTo(ShareStatus.REVOKED);
    assertThat(saved.getValue().tokenDigest()).isEqualTo(existing.tokenDigest());
  }

  @Test
  void rejectsRevokingShareOwnedByAnotherIssuer() {
    UUID shareId = UUID.randomUUID();
    when(repository.findById(shareId)).thenReturn(Mono.just(
        entity(shareId, UUID.randomUUID(), ShareStatus.ACTIVE)));

    StepVerifier.create(service.revoke(issuer, shareId))
        .expectError(NotFoundException.class)
        .verify();
    verify(repository, never()).save(any());
  }

  @Test
  void rejectsRevokingShareThatIsAlreadyInactive() {
    UUID shareId = UUID.randomUUID();
    when(repository.findById(shareId)).thenReturn(Mono.just(
        entity(shareId, issuer, ShareStatus.REVOKED)));

    StepVerifier.create(service.revoke(issuer, shareId))
        .expectErrorSatisfies(error -> {
          assertThat(error).isInstanceOf(ConflictException.class);
          assertThat(((ConflictException) error).code()).isEqualTo("share.not_active");
        })
        .verify();
    verify(repository, never()).save(any());
  }

  private ShareEntity entity(UUID id, UUID owner, ShareStatus status) {
    return new ShareEntity(id, owner, "RESOURCE", target, ShareGranteeType.LINK_TOKEN,
        null, "read", "digest", Instant.now().plusSeconds(3600), status,
        Instant.now().minusSeconds(10), Instant.now(), 0L);
  }
}
