package run.ikaros.sharing;

import java.time.Instant;
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
  public PersistentInviteService(InviteRepository invites, RoomRepository rooms, RoomMembershipRepository members) { this.invites = invites; this.rooms = rooms; this.members = members; }
  public Mono<InviteView> create(UUID actor, UUID roomId, CreateInviteRequest request) {
    return rooms.findById(roomId).filter(room -> room.ownerId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Room 不存在或无权邀请"))).flatMap(room -> invites.findByIssuerIdAndIdempotencyKey(actor, request.idempotencyKey()).switchIfEmpty(Mono.defer(() -> { Instant now = Instant.now(); if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) return Mono.error(new IllegalArgumentException("Invite 过期时间必须在未来")); return invites.save(new InviteEntity(null, roomId, actor, request.inviteeId(), request.role() == null ? RoomRole.MEMBER : request.role(), request.idempotencyKey(), InviteStatus.PENDING, request.expiresAt(), now, now, null)); }))).map(this::view);
  }
  public Flux<InviteView> list(UUID actor) { return invites.findAllByInviteeIdOrderByCreatedAtDesc(actor).take(100).map(this::view); }
  public Mono<InviteView> accept(UUID actor, UUID id) { return invite(actor, id).flatMap(invite -> { if (invite.status() != InviteStatus.PENDING) return Mono.error(new ConflictException("Invite 当前不可接受")); return members.findByRoomIdAndPrincipalId(invite.roomId(), actor).flatMap(member -> members.save(new RoomMembershipEntity(member.id(), member.roomId(), member.principalId(), invite.role(), member.joinedAt(), null, Instant.now(), member.version()))).switchIfEmpty(members.save(new RoomMembershipEntity(null, invite.roomId(), actor, invite.role(), Instant.now(), null, Instant.now(), null))).then(invites.save(new InviteEntity(invite.id(), invite.roomId(), invite.issuerId(), invite.inviteeId(), invite.role(), invite.idempotencyKey(), InviteStatus.ACCEPTED, invite.expiresAt(), invite.createdAt(), Instant.now(), invite.version()))); }).map(this::view); }
  public Mono<InviteView> decline(UUID actor, UUID id) { return invite(actor, id).flatMap(invite -> update(invite, InviteStatus.DECLINED)).map(this::view); }
  public Mono<InviteView> revoke(UUID actor, UUID id) { return invites.findById(id).filter(invite -> invite.issuerId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Invite 不存在或无权撤销"))).flatMap(invite -> update(invite, InviteStatus.REVOKED)).map(this::view); }
  private Mono<InviteEntity> invite(UUID actor, UUID id) { return invites.findById(id).filter(invite -> invite.inviteeId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Invite 不存在"))); }
  private Mono<InviteEntity> update(InviteEntity invite, InviteStatus status) { return invite.status() != InviteStatus.PENDING ? Mono.error(new ConflictException("Invite 当前不可变更")) : invites.save(new InviteEntity(invite.id(), invite.roomId(), invite.issuerId(), invite.inviteeId(), invite.role(), invite.idempotencyKey(), status, invite.expiresAt(), invite.createdAt(), Instant.now(), invite.version())); }
  private InviteView view(InviteEntity invite) { return new InviteView(invite.id(), invite.roomId(), invite.issuerId(), invite.inviteeId(), invite.role(), invite.idempotencyKey(), invite.status(), invite.expiresAt(), invite.createdAt()); }
}
