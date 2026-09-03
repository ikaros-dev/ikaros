package run.ikaros.sharing;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentRoomEventService implements RoomEventService {
  private final RoomRepository rooms;
  private final RoomMembershipRepository members;
  private final RoomEventRepository events;

  public PersistentRoomEventService(RoomRepository rooms, RoomMembershipRepository members, RoomEventRepository events) {
    this.rooms = rooms; this.members = members; this.events = events;
  }

  public Mono<RoomEventView> append(UUID actor, UUID roomId, AppendRoomEventRequest request) {
    return access(actor, roomId).flatMap(room -> {
      if (room.status() != RoomStatus.ACTIVE) return Mono.error(new ConflictException("Room 当前不可修改"));
      if (request.expectedStateVersion() != null && request.expectedStateVersion() != room.stateVersion())
        return Mono.error(new ConflictException("Room State Version 冲突"));
      long sequence = room.sequence() + 1, stateVersion = room.stateVersion() + 1;
      Instant now = Instant.now();
      return rooms.save(new RoomEntity(room.id(), room.ownerId(), room.kind(), room.targetType(), room.targetId(),
          room.visibility(), room.status(), stateVersion, sequence, room.expiresAt(), room.createdAt(), now, room.version()))
          .then(events.save(new RoomEventEntity(null, roomId, sequence, request.eventType(), actor, now,
              request.payload(), stateVersion))).map(this::view);
    });
  }

  public Flux<RoomEventView> replay(UUID actor, UUID roomId, long after) {
    return access(actor, roomId).thenMany(events.findAllByRoomIdAndSequenceGreaterThanOrderBySequenceAsc(roomId, after)
        .take(100).map(this::view));
  }

  private Mono<RoomEntity> access(UUID actor, UUID roomId) {
    return rooms.findById(roomId).flatMap(room -> room.ownerId().equals(actor) ? Mono.just(room)
        : members.findByRoomIdAndPrincipalId(roomId, actor).filter(m -> m.leftAt() == null).thenReturn(room))
        .switchIfEmpty(Mono.error(new NotFoundException("Room 不存在或无权访问")));
  }

  private RoomEventView view(RoomEventEntity event) {
    return new RoomEventView(event.eventId(), event.roomId(), event.sequence(), event.eventType(), event.actorId(),
        event.occurredAt(), event.payload(), event.stateVersion());
  }
}
