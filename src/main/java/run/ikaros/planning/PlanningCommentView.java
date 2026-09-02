package run.ikaros.planning; import java.time.Instant; import java.util.UUID;
public record PlanningCommentView(UUID id,UUID authorId,PlanningCommentTargetType targetType,UUID targetId,String content,Instant createdAt,Instant updatedAt,Instant deletedAt,long version) {}
