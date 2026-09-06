package run.ikaros.planning; import java.time.Instant; import java.util.UUID;
public record PlanningProjectMemberView(UUID id,UUID projectId,UUID userId,PlanningProjectMemberRole role,Instant createdAt) {}
