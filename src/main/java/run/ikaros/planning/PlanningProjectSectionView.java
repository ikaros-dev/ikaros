package run.ikaros.planning; import java.time.Instant; import java.util.UUID;
public record PlanningProjectSectionView(UUID id,UUID projectId,String name,int position,Instant createdAt,Instant updatedAt,long version) {}
