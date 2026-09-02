package run.ikaros.planning; import java.time.Instant; import java.util.UUID;
public record OkrCycleView(UUID id,UUID ownerId,String name,Instant startAt,Instant endAt,OkrStatus status,Instant createdAt,Instant updatedAt,long version) {}
