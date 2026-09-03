package run.ikaros.planning; import java.time.Instant; import java.util.UUID;
public record OkrObjectiveView(UUID id,UUID ownerId,UUID cycleId,String title,String description,UUID goalId,OkrStatus status,Instant createdAt,Instant updatedAt,long version) {}
