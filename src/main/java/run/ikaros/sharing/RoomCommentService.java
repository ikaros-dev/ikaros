package run.ikaros.sharing;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class RoomCommentService {
  private final RoomRepository rooms;
  private final RoomMembershipRepository members;
  private final RoomCommentRepository comments;
  public RoomCommentService(RoomRepository rooms, RoomMembershipRepository members, RoomCommentRepository comments) { this.rooms = rooms; this.members = members; this.comments = comments; }
  public Mono<RoomCommentView> publish(UUID actor, UUID roomId, PublishRoomCommentRequest request) { return access(actor, roomId).then(comments.findByRoomIdAndAuthorIdAndIdempotencyKey(roomId, actor, request.idempotencyKey()).switchIfEmpty(Mono.defer(() -> { Instant now = Instant.now(); return comments.save(new RoomCommentEntity(null, roomId, actor, request.idempotencyKey(), request.body(), false, now, now, null)); }))).map(this::view); }
  public Flux<RoomCommentView> list(UUID actor, UUID roomId) { return access(actor, roomId).thenMany(comments.findAllByRoomIdAndDeletedFalseOrderByCreatedAtAsc(roomId).take(100)).map(this::view); }
  public Mono<RoomCommentView> edit(UUID actor, UUID id, EditRoomCommentRequest request) { return comments.findById(id).filter(comment -> comment.authorId().equals(actor) && !comment.deleted()).switchIfEmpty(Mono.error(new NotFoundException("Comment 不存在或无权编辑"))).flatMap(comment -> comments.save(new RoomCommentEntity(comment.id(), comment.roomId(), comment.authorId(), comment.idempotencyKey(), request.body(), false, comment.createdAt(), Instant.now(), comment.version()))).map(this::view); }
  public Mono<RoomCommentView> delete(UUID actor, UUID id) { return comments.findById(id).filter(comment -> comment.authorId().equals(actor) && !comment.deleted()).switchIfEmpty(Mono.error(new NotFoundException("Comment 不存在或无权删除"))).flatMap(comment -> comments.save(new RoomCommentEntity(comment.id(), comment.roomId(), comment.authorId(), comment.idempotencyKey(), comment.body(), true, comment.createdAt(), Instant.now(), comment.version()))).map(this::view); }
  private Mono<Void> access(UUID actor, UUID roomId) { return rooms.findById(roomId).flatMap(room -> members.findByRoomIdAndPrincipalId(roomId, actor).filter(member -> member.leftAt() == null).then()).switchIfEmpty(Mono.error(new NotFoundException("Room 不存在或无权访问"))); }
  private RoomCommentView view(RoomCommentEntity comment) { return new RoomCommentView(comment.id(), comment.roomId(), comment.authorId(), comment.body(), comment.deleted(), comment.createdAt(), comment.updatedAt()); }
}
