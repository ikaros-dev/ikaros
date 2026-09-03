package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.identity.PlatformUserRepository;

@Service
public class PersistentPlanningProjectMemberService implements PlanningProjectMemberService {
  private final PlanningProjectRepository projects;
  private final PlanningProjectMemberRepository members;
  private final PlatformUserRepository users;
  public PersistentPlanningProjectMemberService(PlanningProjectRepository projects, PlanningProjectMemberRepository members, PlatformUserRepository users) { this.projects = projects; this.members = members; this.users = users; }
  public Mono<PlanningProjectMemberView> add(UUID owner, UUID project, AddPlanningProjectMemberRequest request) { return owned(owner, project).then(users.findById(request.userId()).switchIfEmpty(Mono.error(new NotFoundException("User 不存在")))).then(members.findByProjectIdAndUserId(project, request.userId()).flatMap(existing -> Mono.<PlanningProjectMemberView>error(new ConflictException("Project 成员已存在"))).switchIfEmpty(members.save(new PlanningProjectMemberEntity(null, project, request.userId(), request.role(), Instant.now())).map(this::view))); }
  public Flux<PlanningProjectMemberView> list(UUID actor, UUID project) { return readable(actor, project).thenMany(members.findAllByProjectId(project).take(100)).map(this::view); }
  public Mono<Void> remove(UUID owner, UUID project, UUID user) { return owned(owner, project).then(members.findByProjectIdAndUserId(project, user).flatMap(members::delete).then()).then(); }
  private Mono<PlanningProjectEntity> owned(UUID owner, UUID id) { return projects.findById(id).filter(project -> project.ownerId().equals(owner)).switchIfEmpty(Mono.error(new NotFoundException("Project 不存在"))); }
  private Mono<Void> readable(UUID actor, UUID projectId) { return projects.findById(projectId).flatMap(project -> project.ownerId().equals(actor) ? Mono.just(project) : members.findByProjectIdAndUserId(projectId, actor).map(member -> project)).switchIfEmpty(Mono.error(new NotFoundException("Project 不存在或无权访问"))).then(); }
  private PlanningProjectMemberView view(PlanningProjectMemberEntity member) { return new PlanningProjectMemberView(member.id(), member.projectId(), member.userId(), member.role(), member.createdAt()); }
}
