package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentPlanningProjectSectionService implements PlanningProjectSectionService {
  private final PlanningProjectRepository projects;
  private final PlanningProjectSectionRepository sections;
  private final PlanningProjectMemberRepository members;

  public PersistentPlanningProjectSectionService(PlanningProjectRepository p, PlanningProjectSectionRepository s,
      PlanningProjectMemberRepository m) {
    projects = p;
    sections = s;
    members = m;
  }

  @Override
  public Mono<PlanningProjectSectionView> create(UUID actor, UUID projectId, CreatePlanningProjectSectionRequest request) {
    return requireManage(actor, projectId).then(Mono.defer(() -> {
      Instant now = Instant.now();
      return sections.save(new PlanningProjectSectionEntity(null, projectId, request.name().trim(),
          request.position() == null ? 0 : request.position(), now, now, null));
    })).map(this::view);
  }

  @Override
  public Flux<PlanningProjectSectionView> list(UUID actor, UUID projectId) {
    return requireMember(actor, projectId)
        .thenMany(sections.findAllByProjectIdOrderByPositionAsc(projectId)).map(this::view);
  }

  @Override
  public Mono<PlanningProjectSectionView> update(UUID actor, UUID id, UpdatePlanningProjectSectionRequest request) {
    return section(id).flatMap(old -> requireManage(actor, old.projectId()).then(Mono.defer(() -> {
      if ((old.version() == null ? 0 : old.version()) != request.expectedVersion()) {
        return Mono.error(new ConflictException("Section 版本冲突"));
      }
      return sections.save(new PlanningProjectSectionEntity(old.id(), old.projectId(), request.name().trim(),
          request.position() == null ? old.position() : request.position(), old.createdAt(), Instant.now(), old.version()));
    }))).map(this::view);
  }

  @Override
  public Mono<Void> delete(UUID actor, UUID id) {
    return section(id).flatMap(section -> requireManage(actor, section.projectId()).then(sections.delete(section))).then();
  }

  private Mono<PlanningProjectSectionEntity> section(UUID id) {
    return sections.findById(id).switchIfEmpty(Mono.error(new NotFoundException("Section 不存在")));
  }

  private Mono<Void> requireMember(UUID actor, UUID projectId) {
    return projectRole(actor, projectId).then();
  }

  private Mono<Void> requireManage(UUID actor, UUID projectId) {
    return projectRole(actor, projectId)
        .filter(role -> role == PlanningProjectMemberRole.MANAGE_PROJECT)
        .switchIfEmpty(Mono.error(new NotFoundException("Project 不存在或无项目管理权限"))).then();
  }

  private Mono<PlanningProjectMemberRole> projectRole(UUID actor, UUID projectId) {
    return projects.findById(projectId)
        .flatMap(project -> project.ownerId().equals(actor)
            ? Mono.just(PlanningProjectMemberRole.MANAGE_PROJECT)
            : members.findByProjectIdAndUserId(projectId, actor).map(PlanningProjectMemberEntity::role))
        .switchIfEmpty(Mono.error(new NotFoundException("Project 不存在或无权访问")));
  }

  private PlanningProjectSectionView view(PlanningProjectSectionEntity x) {
    return new PlanningProjectSectionView(x.id(), x.projectId(), x.name(), x.position(), x.createdAt(), x.updatedAt(),
        x.version() == null ? 0 : x.version());
  }
}
