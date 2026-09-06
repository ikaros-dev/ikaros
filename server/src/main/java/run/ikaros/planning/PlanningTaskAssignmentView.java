package run.ikaros.planning; import java.time.Instant; import java.util.UUID;
public record PlanningTaskAssignmentView(UUID id,UUID taskId,UUID assigneeId,UUID assignedBy,Instant createdAt) {}
