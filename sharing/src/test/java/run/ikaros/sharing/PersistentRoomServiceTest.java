package run.ikaros.sharing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PersistentRoomServiceTest {
  @Mock private RoomRepository rooms;
  @Mock private RoomMembershipRepository memberships;
  @Mock private TransactionalOperator transaction;

  private PersistentRoomService service;
  private UUID owner;
  private UUID target;

  @BeforeEach
  void setUp() {
    service = new PersistentRoomService(rooms, memberships, transaction);
    lenient().when(transaction.transactional(any(Mono.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    owner = UUID.randomUUID();
    target = UUID.randomUUID();
  }

  @Test
  void createsRoomAndPersistsOwnerMembership() {
    UUID roomId = UUID.randomUUID();
    when(rooms.save(any(RoomEntity.class))).thenAnswer(invocation -> {
      RoomEntity room = invocation.getArgument(0);
      return Mono.just(new RoomEntity(roomId, room.ownerId(), room.kind(), room.targetType(), room.targetId(),
          room.visibility(), room.status(), room.stateVersion(), room.sequence(), room.expiresAt(),
          room.createdAt(), room.updatedAt(), 0L));
    });
    when(memberships.save(any(RoomMembershipEntity.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(service.create(owner, new CreateRoomRequest(
            "WATCH", "RESOURCE", target, "INVITE_ONLY", Instant.now().plusSeconds(3600))))
        .assertNext(view -> {
          assertThat(view.id()).isEqualTo(roomId);
          assertThat(view.ownerId()).isEqualTo(owner);
          assertThat(view.kind()).isEqualTo("WATCH");
          assertThat(view.status()).isEqualTo(RoomStatus.ACTIVE);
        })
        .verifyComplete();

    ArgumentCaptor<RoomMembershipEntity> saved = ArgumentCaptor.forClass(RoomMembershipEntity.class);
    verify(memberships).save(saved.capture());
    assertThat(saved.getValue().roomId()).isEqualTo(roomId);
    assertThat(saved.getValue().principalId()).isEqualTo(owner);
    assertThat(saved.getValue().role()).isEqualTo(RoomRole.OWNER);
  }

  @Test
  void rejectsPastExpiryBeforeWriting() {
    StepVerifier.create(service.create(owner, new CreateRoomRequest(
            "WATCH", "RESOURCE", target, "INVITE_ONLY", Instant.now().minusSeconds(1))))
        .expectErrorMessage("Room 过期时间必须在未来")
        .verify();

    verify(rooms, never()).save(any());
    verify(memberships, never()).save(any());
  }

  @Test
  void rejectsIncompleteRoomBeforeWriting() {
    StepVerifier.create(service.create(owner, new CreateRoomRequest("WATCH", "RESOURCE", null, "PRIVATE", null)))
        .expectErrorMessage("Room 创建信息不完整")
        .verify();

    verify(rooms, never()).save(any());
    verify(memberships, never()).save(any());
  }

  @Test
  void rejectsMissingActorBeforeWriting() {
    StepVerifier.create(service.create(null,
            new CreateRoomRequest("WATCH", "RESOURCE", target, "PRIVATE", null)))
        .expectErrorMessage("Room 创建人不能为空")
        .verify();

    verify(rooms, never()).save(any());
    verify(memberships, never()).save(any());
  }

  @Test
  void trimsCreationFieldsAndDefaultsVisibility() {
    UUID roomId = UUID.randomUUID();
    when(rooms.save(any(RoomEntity.class))).thenAnswer(invocation -> {
      RoomEntity room = invocation.getArgument(0);
      assertThat(room.ownerId()).isEqualTo(owner);
      assertThat(room.kind()).isEqualTo("WATCH");
      assertThat(room.targetType()).isEqualTo("RESOURCE");
      assertThat(room.visibility()).isEqualTo("PRIVATE");
      return Mono.just(new RoomEntity(roomId, room.ownerId(), room.kind(), room.targetType(), room.targetId(),
          room.visibility(), room.status(), room.stateVersion(), room.sequence(), room.expiresAt(),
          room.createdAt(), room.updatedAt(), 0L));
    });
    when(memberships.save(any(RoomMembershipEntity.class)))
        .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

    StepVerifier.create(service.create(owner,
            new CreateRoomRequest(" WATCH ", " RESOURCE ", target, null, null)))
        .assertNext(view -> assertThat(view.id()).isEqualTo(roomId))
        .verifyComplete();
  }

  @Test
  void rejectsUnknownVisibilityBeforeWriting() {
    StepVerifier.create(service.create(owner, new CreateRoomRequest("WATCH", "RESOURCE", target, "FRIENDS", null)))
        .expectErrorMessage("Room 可见性不合法")
        .verify();

    verify(rooms, never()).save(any());
    verify(memberships, never()).save(any());
  }
}
