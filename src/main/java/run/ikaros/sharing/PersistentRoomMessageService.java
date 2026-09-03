package run.ikaros.sharing;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentRoomMessageService implements RoomMessageService {
  private final RoomRepository rooms;
  private final RoomMembershipRepository members;
  private final RoomMessageRepository messages;
  public PersistentRoomMessageService(RoomRepository rooms, RoomMembershipRepository members, RoomMessageRepository messages) { this.rooms = rooms; this.members = members; this.messages = messages; }
  public Mono<RoomMessageView> send(UUID actor, UUID roomId, SendRoomMessageRequest request) {
    return access(actor, roomId).flatMap(room -> messages.findByRoomIdAndAuthorIdAndIdempotencyKey(roomId, actor, request.idempotencyKey())
        .switchIfEmpty(Mono.defer(() -> messages.save(new RoomMessageEntity(null, roomId, actor, request.idempotencyKey(), request.body(), Instant.now(), null))))).map(this::view);
  }
  public Flux<RoomMessageView> list(UUID actor, UUID roomId) { return access(actor, roomId).thenMany(messages.findAllByRoomIdOrderByCreatedAtAsc(roomId).take(100)).map(this::view); }
  private Mono<Void> access(UUID actor, UUID roomId) { return rooms.findById(roomId).flatMap(room -> members.findByRoomIdAndPrincipalId(roomId, actor).filter(m -> m.leftAt() == null).then()).switchIfEmpty(Mono.error(new NotFoundException("Room 不存在或无权访问"))); }
  private RoomMessageView view(RoomMessageEntity message) { return new RoomMessageView(message.id(), message.roomId(), message.authorId(), message.body(), message.createdAt()); }
}
