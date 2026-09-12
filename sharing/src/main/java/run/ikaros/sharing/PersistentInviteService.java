package run.ikaros.sharing;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentInviteService implements InviteService {
  private final InviteRepository invites;
  private final RoomRepository rooms;
  private final RoomMembershipRepository members;

  public PersistentInviteService(InviteRepository invites, RoomRepository rooms,
      RoomMembershipRepository members) {
    this.invites = invites;
    this.rooms = rooms;
    this.members = members;
  }

  @Override
  public Mono<InviteView> create(UUID actor, UUID roomId, CreateInviteRequest request) {
    if (request == null || request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
      return Mono.error(new IllegalArgumentException("Invite 幂等键不能为空"));
    }
    RoomRole role = request.role() == null ? RoomRole.MEMBER : request.role();
    return rooms.findById(roomId)
        .filter(room -> room.ownerId().equals(actor) && room.status() == RoomStatus.ACTIVE)
        .switchIfEmpty(Mono.error(new NotFoundException("Room 不存在、已关闭或无权邀请")))
        .flatMap(room -> invites.findByIssuerIdAndIdempotencyKey(actor, request.idempotencyKey())
            .flatMap(existing -> sameRequest(existing, roomId, request.inviteeId(), role,
                request.expiresAt()) ? Mono.just(existing)
                : Mono.error(new ConflictException("Invite 幂等键已用于其他请求")))
            .switchIfEmpty(Mono.defer(() -> {
              Instant now = Instant.now();
              if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
                return Mono.error(new IllegalArgumentException("Invite 过期时间必须在未来"));
              }
              return invites.save(new InviteEntity(null, roomId, actor, request.inviteeId(), role,
                  request.idempotencyKey(), InviteStatus.PENDING, request.expiresAt(), now, now, null));
            })))
        .map(this::view);
  }

  @Override
  public Flux<InviteView> list(UUID actor) {
    return invites.findAllByInviteeIdOrderByCreatedAtDesc(actor).take(100)
        .flatMap(this::expireIfNeeded).map(this::view);
  }

  @Override
  public Mono<InviteView> accept(UUID actor, UUID id) {
    return invite(actor, id).flatMap(invite -> rooms.findById(invite.roomId())
        .switchIfEmpty(Mono.error(new NotFoundException("Room 不存在")))
        .flatMap(room -> {
          if (room.status() != RoomStatus.ACTIVE) return Mono.error(new ConflictException("Room 当前不可加入"));
          if (invite.status() != InviteStatus.PENDING) return Mono.error(new ConflictException("Invite 当前不可接受"));
          if (expired(invite)) return expire(invite).then(Mono.error(new ConflictException("Invite 已过期")));
          Instant now = Instant.now();
          return members.findByRoomIdAndPrincipalId(invite.roomId(), actor)
              .flatMap(member -> members.save(new RoomMembershipEntity(member.id(), member.roomId(),
                  member.principalId(), invite.role(), member.joinedAt(), null, now, member.version())))
              .switchIfEmpty(members.save(new RoomMembershipEntity(null, invite.roomId(), actor,
                  invite.role(), now, null, now, null)))
              .then(invites.save(new InviteEntity(invite.id(), invite.roomId(), invite.issuerId(),
                  invite.inviteeId(), invite.role(), invite.idempotencyKey(), InviteStatus.ACCEPTED,
                  invite.expiresAt(), invite.createdAt(), now, invite.version())));
        })).map(this::view);
  }

  @Override
  public Mono<InviteView> decline(UUID actor, UUID id) {
    return invite(actor, id).flatMap(invite -> update(invite, InviteStatus.DECLINED)).map(this::view);
  }

  @Override
  public Mono<InviteView> revoke(UUID actor, UUID id) {
    return invites.findById(id).filter(invite -> invite.issuerId().equals(actor))
        .switchIfEmpty(Mono.error(new NotFoundException("Invite 不存在或无权撤销")))
        .flatMap(invite -> update(invite, InviteStatus.REVOKED)).map(this::view);
  }

  private Mono<InviteEntity> invite(UUID actor, UUID id) {
    return invites.findById(id).filter(invite -> invite.inviteeId().equals(actor))
        .switchIfEmpty(Mono.error(new NotFoundException("Invite 不存在")));
  }

  private Mono<InviteEntity> update(InviteEntity invite, InviteStatus status) {
    if (invite.status() != InviteStatus.PENDING) return Mono.error(new ConflictException("Invite 当前不可变更"));
    if (expired(invite)) return expire(invite).then(Mono.error(new ConflictException("Invite 已过期")));
    return invites.save(new InviteEntity(invite.id(), invite.roomId(), invite.issuerId(),
        invite.inviteeId(), invite.role(), invite.idempotencyKey(), status, invite.expiresAt(),
        invite.createdAt(), Instant.now(), invite.version()));
  }

  private Mono<InviteEntity> expireIfNeeded(InviteEntity invite) {
    return invite.status() == InviteStatus.PENDING && expired(invite) ? expire(invite) : Mono.just(invite);
  }

  private Mono<InviteEntity> expire(InviteEntity invite) {
    return invites.save(new InviteEntity(invite.id(), invite.roomId(), invite.issuerId(),
        invite.inviteeId(), invite.role(), invite.idempotencyKey(), InviteStatus.EXPIRED,
        invite.expiresAt(), invite.createdAt(), Instant.now(), invite.version()));
  }

  private boolean expired(InviteEntity invite) {
    return invite.expiresAt() != null && !invite.expiresAt().isAfter(Instant.now());
  }

  private boolean sameRequest(InviteEntity existing, UUID roomId, UUID inviteeId, RoomRole role,
      Instant expiresAt) {
    return existing.roomId().equals(roomId) && existing.inviteeId().equals(inviteeId)
        && existing.role() == role && Objects.equals(existing.expiresAt(), expiresAt);
  }

  private InviteView view(InviteEntity invite) {
    return new InviteView(invite.id(), invite.roomId(), invite.issuerId(), invite.inviteeId(),
        invite.role(), invite.idempotencyKey(), invite.status(), invite.expiresAt(), invite.createdAt());
  }
}
