package run.ikaros.planning; import java.time.Instant; import java.util.UUID;
public record PlanningImportantDateView(UUID id,UUID ownerId,String title,String description,Instant occursAt,String timeZone,String kind,PlanningImportantDateStatus status,Instant createdAt,Instant updatedAt,long version) {}
