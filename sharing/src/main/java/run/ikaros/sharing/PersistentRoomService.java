package run.ikaros.sharing;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentRoomService implements RoomService {
  private final RoomRepository rooms;
  private final RoomMembershipRepository members;
  private final TransactionalOperator transaction;

  public PersistentRoomService(RoomRepository rooms, RoomMembershipRepository members,
      TransactionalOperator transaction) {
    this.rooms = rooms;
    this.members = members;
    this.transaction = transaction;
  }

  public Mono<RoomView> create(UUID actor, CreateRoomRequest request) {
    if (actor == null) return Mono.error(new IllegalArgumentException("Room 创建人不能为空"));
    if (request == null || request.kind() == null || request.kind().isBlank()
        || request.targetType() == null || request.targetType().isBlank() || request.targetId() == null) {
      return Mono.error(new IllegalArgumentException("Room 创建信息不完整"));
    }
    Instant now = Instant.now();
    if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
      return Mono.error(new IllegalArgumentException("Room 过期时间必须在未来"));
    }
    String visibility = normalizeVisibility(request.visibility());
    if (visibility == null) return Mono.error(new IllegalArgumentException("Room 可见性不合法"));
    RoomEntity draft = new RoomEntity(null, actor, request.kind().trim(), request.targetType().trim(),
        request.targetId(), visibility, RoomStatus.ACTIVE, 0, 0, request.expiresAt(), now, now, null);
    return transaction.transactional(rooms.save(draft).flatMap(saved -> members
        .save(new RoomMembershipEntity(null, saved.id(), actor, RoomRole.OWNER, now, null, now, null))
        .thenReturn(view(saved))));
  }

  public Flux<RoomView> list(UUID actor) {
    return rooms.findAllByOwnerIdOrderByCreatedAtDesc(actor).take(100).map(this::view);
  }

  public Mono<RoomView> lock(UUID actor, UUID id) { return owner(actor, id).flatMap(room -> change(room, RoomStatus.LOCKED)); }
  public Mono<RoomView> unlock(UUID actor, UUID id) { return owner(actor, id).flatMap(room -> change(room, RoomStatus.ACTIVE)); }
  public Mono<RoomView> end(UUID actor, UUID id) { return owner(actor, id).flatMap(room -> change(room, RoomStatus.ENDED)); }

  /** Direct joining is intentionally restricted to PUBLIC rooms; invitations create membership for other visibilities. */
  public Mono<RoomMembershipView> join(UUID actor, UUID id) {
    return rooms.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Room 不存在")))
        .flatMap(room -> {
          if (room.status() != RoomStatus.ACTIVE) return Mono.error(new ConflictException("Room 当前不可加入"));
          if (!"PUBLIC".equals(room.visibility())) return Mono.error(new ConflictException("Room 需要有效邀请才能加入"));
          return members.findByRoomIdAndPrincipalId(id, actor)
              .flatMap(member -> member.leftAt() == null ? Mono.just(member)
                  : members.save(new RoomMembershipEntity(member.id(), member.roomId(), member.principalId(),
                      RoomRole.MEMBER, member.joinedAt(), null, Instant.now(), member.version())))
              .switchIfEmpty(members.save(new RoomMembershipEntity(null, id, actor, RoomRole.MEMBER,
                  Instant.now(), null, Instant.now(), null)))
              .map(this::memberView);
        });
  }

  public Mono<RoomMembershipView> leave(UUID actor, UUID id) {
    return membership(id, actor).flatMap(member -> {
      if (member.role() == RoomRole.OWNER) return Mono.error(new ConflictException("Owner 必须先转移所有权或结束 Room"));
      return members.save(new RoomMembershipEntity(member.id(), member.roomId(), member.principalId(),
          member.role(), member.joinedAt(), Instant.now(), Instant.now(), member.version()));
    }).map(this::memberView);
  }

  public Flux<RoomMembershipView> members(UUID actor, UUID id) {
    return access(actor, id).thenMany(members.findAllByRoomIdAndLeftAtIsNull(id).take(100).map(this::memberView));
  }

  public Mono<RoomMembershipView> changeRole(UUID actor, UUID id, UUID principal, RoomRole role) {
    return owner(actor, id).then(membership(id, principal)).flatMap(member -> members.save(
        new RoomMembershipEntity(member.id(), member.roomId(), member.principalId(),
            role == RoomRole.OWNER ? RoomRole.MEMBER : role, member.joinedAt(), member.leftAt(),
            Instant.now(), member.version()))).map(this::memberView);
  }

  public Mono<RoomMembershipView> transferOwnership(UUID actor, UUID id, UUID principal) {
    return owner(actor, id).flatMap(room -> membership(id, principal).flatMap(next ->
        members.findByRoomIdAndPrincipalId(id, actor).flatMap(old -> members.save(
            new RoomMembershipEntity(old.id(), old.roomId(), old.principalId(), RoomRole.MEMBER,
                old.joinedAt(), old.leftAt(), Instant.now(), old.version())))
            .then(members.save(new RoomMembershipEntity(next.id(), next.roomId(), next.principalId(),
                RoomRole.OWNER, next.joinedAt(), next.leftAt(), Instant.now(), next.version())))
            .flatMap(saved -> rooms.save(new RoomEntity(room.id(), principal, room.kind(), room.targetType(),
                room.targetId(), room.visibility(), room.status(), room.stateVersion() + 1, room.sequence(),
                room.expiresAt(), room.createdAt(), Instant.now(), room.version())).thenReturn(saved))))
        .map(this::memberView);
  }

  public Mono<RoomMembershipView> remove(UUID actor, UUID id, UUID principal) {
    return owner(actor, id).then(membership(id, principal)).flatMap(member -> {
      if (member.role() == RoomRole.OWNER) return Mono.error(new ConflictException("不能移除 Owner"));
      return members.save(new RoomMembershipEntity(member.id(), member.roomId(), member.principalId(),
          member.role(), member.joinedAt(), Instant.now(), Instant.now(), member.version()));
    }).map(this::memberView);
  }

  private Mono<RoomEntity> owner(UUID actor, UUID id) {
    return rooms.findById(id).filter(room -> room.ownerId().equals(actor))
        .switchIfEmpty(Mono.error(new NotFoundException("Room 不存在或无权操作")));
  }

  private Mono<Void> access(UUID actor, UUID id) {
    return members.findByRoomIdAndPrincipalId(id, actor).filter(member -> member.leftAt() == null)
        .switchIfEmpty(Mono.error(new NotFoundException("Room 不存在或无权访问"))).then();
  }

  private Mono<RoomMembershipEntity> membership(UUID id, UUID principal) {
    return members.findByRoomIdAndPrincipalId(id, principal).filter(member -> member.leftAt() == null)
        .switchIfEmpty(Mono.error(new NotFoundException("Room 成员不存在")));
  }

  private Mono<RoomView> change(RoomEntity room, RoomStatus status) {
    return rooms.save(new RoomEntity(room.id(), room.ownerId(), room.kind(), room.targetType(), room.targetId(),
        room.visibility(), status, room.stateVersion() + 1, room.sequence(), room.expiresAt(), room.createdAt(),
        Instant.now(), room.version())).map(this::view);
  }

  private RoomView view(RoomEntity room) {
    return new RoomView(room.id(), room.ownerId(), room.kind(), room.targetType(), room.targetId(), room.visibility(),
        room.status(), room.stateVersion(), room.sequence(), room.expiresAt(), room.createdAt());
  }

  private RoomMembershipView memberView(RoomMembershipEntity member) {
    return new RoomMembershipView(member.id(), member.roomId(), member.principalId(), member.role(),
        member.joinedAt(), member.leftAt());
  }

  private String normalizeVisibility(String value) {
    if (value == null || value.isBlank()) return "PRIVATE";
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    return switch (normalized) {
      case "PRIVATE", "PUBLIC", "INVITE_ONLY" -> normalized;
      default -> null;
    };
  }
}
