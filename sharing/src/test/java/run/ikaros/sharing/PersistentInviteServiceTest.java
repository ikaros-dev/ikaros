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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PersistentInviteServiceTest {
  @Mock private InviteRepository invites;
  @Mock private RoomRepository rooms;
  @Mock private RoomMembershipRepository members;

  private PersistentInviteService service;
  private UUID owner;
  private UUID invitee;
  private UUID roomId;

  @BeforeEach
  void setUp() {
    service = new PersistentInviteService(invites, rooms, members);
    owner = UUID.randomUUID();
    invitee = UUID.randomUUID();
    roomId = UUID.randomUUID();
  }

  @Test
  void createsInviteUsingBodyIdempotencyKey() {
    RoomEntity room = room(RoomStatus.ACTIVE, "INVITE_ONLY");
    when(rooms.findById(roomId)).thenReturn(Mono.just(room));
    when(invites.findByIssuerIdAndIdempotencyKey(owner, "key")).thenReturn(Mono.empty());
    when(invites.save(any(InviteEntity.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

    StepVerifier.create(service.create(owner, roomId,
            new CreateInviteRequest(invitee, RoomRole.MEMBER, "key", Instant.now().plusSeconds(60))))
        .assertNext(view -> {
          assertThat(view.inviteeId()).isEqualTo(invitee);
          assertThat(view.status()).isEqualTo(InviteStatus.PENDING);
        }).verifyComplete();
  }

  @Test
  void rejectsSameIdempotencyKeyWithDifferentPayload() {
    RoomEntity room = room(RoomStatus.ACTIVE, "INVITE_ONLY");
    InviteEntity existing = invite(InviteStatus.PENDING, invitee, RoomRole.MEMBER, "key",
        Instant.now().plusSeconds(60));
    when(rooms.findById(roomId)).thenReturn(Mono.just(room));
    when(invites.findByIssuerIdAndIdempotencyKey(owner, "key")).thenReturn(Mono.just(existing));

    StepVerifier.create(service.create(owner, roomId,
            new CreateInviteRequest(UUID.randomUUID(), RoomRole.MEMBER, "key", existing.expiresAt())))
        .expectErrorMessage("Invite 幂等键已用于其他请求").verify();
    verify(invites, never()).save(any());
  }

  @Test
  void rejectsExpiredInviteAndPersistsExpiredStatus() {
    InviteEntity existing = invite(InviteStatus.PENDING, invitee, RoomRole.MEMBER, "key",
        Instant.now().minusSeconds(1));
    when(invites.findById(existing.id())).thenReturn(Mono.just(existing));
    when(rooms.findById(roomId)).thenReturn(Mono.just(room(RoomStatus.ACTIVE, "INVITE_ONLY")));
    when(invites.save(any(InviteEntity.class))).thenAnswer(call -> Mono.just(call.getArgument(0)));

    StepVerifier.create(service.accept(invitee, existing.id()))
        .expectErrorMessage("Invite 已过期").verify();
    verify(members, never()).save(any());
    verify(invites).save(any(InviteEntity.class));
  }

  private RoomEntity room(RoomStatus status, String visibility) {
    return new RoomEntity(roomId, owner, "WATCH", "RESOURCE", UUID.randomUUID(), visibility, status,
        0, 0, null, Instant.now(), Instant.now(), 0L);
  }

  private InviteEntity invite(InviteStatus status, UUID target, RoomRole role, String key,
      Instant expiresAt) {
    Instant now = Instant.now();
    return new InviteEntity(UUID.randomUUID(), roomId, owner, target, role, key, status, expiresAt,
        now, now, 0L);
  }
}
