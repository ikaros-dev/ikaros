package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.common.PreconditionFailedException;

@Service
public class PersistentPlanningCommentService implements PlanningCommentService {
  private final PlanningCommentRepository comments;
  private final PlanningTaskRepository tasks;
  private final PlanningProjectRepository projects;
  private final PlanningProjectMemberRepository members;
  private final PlanningGoalRepository goals;

  public PersistentPlanningCommentService(PlanningCommentRepository c, PlanningTaskRepository t,
      PlanningProjectRepository p, PlanningProjectMemberRepository m, PlanningGoalRepository g) {
    comments = c;
    tasks = t;
    projects = p;
    members = m;
    goals = g;
  }

  @Override
  public Mono<PlanningCommentView> create(UUID author, CreatePlanningCommentRequest request) {
    return target(author, request.targetType(), request.targetId(), true).then(Mono.defer(() -> {
      Instant now = Instant.now();
      return comments.save(new PlanningCommentEntity(null, author, request.targetType(), request.targetId(),
          request.content().trim(), now, now, null, null));
    })).map(this::view);
  }

  @Override
  public Flux<PlanningCommentView> list(UUID actor, PlanningCommentTargetType type, UUID id) {
    return target(actor, type, id, false)
        .thenMany(comments.findAllByTargetTypeAndTargetIdAndDeletedAtIsNullOrderByCreatedAtAsc(type, id).take(100))
        .map(this::view);
  }

  @Override
  public Mono<PlanningCommentView> update(UUID author, UUID id, UpdatePlanningCommentRequest request) {
    return owned(author, id).flatMap(old -> {
      if (old.deletedAt() != null) return Mono.error(new ConflictException("已删除评论不能修改"));
      if ((old.version() == null ? 0 : old.version()) != request.expectedVersion()) {
        return Mono.error(new PreconditionFailedException("If-Match 与 Comment 当前版本不匹配"));
      }
      return comments.save(new PlanningCommentEntity(old.id(), old.authorId(), old.targetType(), old.targetId(),
          request.content().trim(), old.createdAt(), Instant.now(), null, old.version()));
    }).map(this::view);
  }

  @Override
  public Mono<Void> delete(UUID author, UUID id) {
    return deleteInternal(author, id, null);
  }

  @Override
  public Mono<Void> delete(UUID author, UUID id, long expectedVersion) {
    return deleteInternal(author, id, expectedVersion);
  }

  private Mono<Void> deleteInternal(UUID author, UUID id, Long expectedVersion) {
    return owned(author, id)
        .flatMap(old -> {
          if (expectedVersion != null && (old.version() == null ? 0 : old.version()) != expectedVersion) {
            return Mono.error(new PreconditionFailedException("If-Match 与 Comment 当前版本不匹配"));
          }
          return comments.save(new PlanningCommentEntity(old.id(), old.authorId(), old.targetType(),
              old.targetId(), old.content(), old.createdAt(), old.updatedAt(), Instant.now(), old.version()));
        })
        .then();
  }

  private Mono<Void> target(UUID actor, PlanningCommentTargetType type, UUID id, boolean write) {
    return switch (type) {
      case TASK -> tasks.findById(id)
          .flatMap(task -> projectAccess(actor, task.projectId(), task.ownerId(), write))
          .switchIfEmpty(Mono.error(new NotFoundException("Target 不存在或无权访问"))).then();
      case PROJECT -> projectAccess(actor, id, null, write);
      case GOAL -> goals.findById(id).filter(goal -> goal.ownerId().equals(actor))
          .switchIfEmpty(Mono.error(new NotFoundException("Target 不存在或无权访问"))).then();
    };
  }

  private Mono<Void> projectAccess(UUID actor, UUID projectId, UUID taskOwner, boolean write) {
    if (projectId == null) {
      return taskOwner != null && taskOwner.equals(actor) ? Mono.empty()
          : Mono.error(new NotFoundException("Target 不存在或无权访问"));
    }
    return projects.findById(projectId)
        .flatMap(project -> project.ownerId().equals(actor)
            ? Mono.just(PlanningProjectMemberRole.MANAGE_PROJECT)
            : members.findByProjectIdAndUserId(projectId, actor).map(PlanningProjectMemberEntity::role))
        .filter(role -> !write || role == PlanningProjectMemberRole.COMMENT
            || role == PlanningProjectMemberRole.MANAGE_PROJECT)
        .switchIfEmpty(Mono.error(new NotFoundException("Target 不存在或无评论权限"))).then();
  }

  private Mono<PlanningCommentEntity> owned(UUID author, UUID id) {
    return comments.findById(id).filter(comment -> comment.authorId().equals(author))
        .switchIfEmpty(Mono.error(new NotFoundException("Comment 不存在")));
  }

  private PlanningCommentView view(PlanningCommentEntity x) {
    return new PlanningCommentView(x.id(), x.authorId(), x.targetType(), x.targetId(), x.content(), x.createdAt(),
        x.updatedAt(), x.deletedAt(), x.version() == null ? 0 : x.version());
  }
}
