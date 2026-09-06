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
public class PersistentPlanningProjectService implements PlanningProjectService {
    private final PlanningProjectRepository repository;
    private final PlanningProjectMemberRepository members;

    public PersistentPlanningProjectService(PlanningProjectRepository repository, PlanningProjectMemberRepository members) {
        this.repository = repository; this.members = members;
    }

    @Override public Mono<PlanningProjectView> create(UUID ownerId, CreatePlanningProjectRequest request) {
        Instant now = Instant.now();
        return repository.save(new PlanningProjectEntity(null, ownerId, request.name().trim(), request.description(),
            PlanningProjectStatus.ACTIVE, now, now, null))
            .flatMap(project -> members.save(new PlanningProjectMemberEntity(null, project.id(), ownerId,
                PlanningProjectMemberRole.MANAGE_PROJECT, now)).thenReturn(project))
            .map(this::view);
    }

    @Override public Flux<PlanningProjectView> list(UUID ownerId) {
        Flux<PlanningProjectEntity> owned = repository.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId).take(100);
        Flux<PlanningProjectEntity> shared = members.findAllByUserId(ownerId).take(100)
            .map(PlanningProjectMemberEntity::projectId)
            .flatMap(repository::findById);
        return Flux.concat(owned, shared).distinct(PlanningProjectEntity::id)
            .sort(java.util.Comparator.comparing(PlanningProjectEntity::updatedAt).reversed())
            .take(100).map(this::view);
    }

    @Override public Mono<PlanningProjectView> update(UUID ownerId, UUID projectId, UpdatePlanningProjectRequest request) {
        return owned(ownerId, projectId).flatMap(old -> {
            check(old, request.expectedVersion());
            return repository.save(new PlanningProjectEntity(old.id(), old.ownerId(), request.name().trim(),
                request.description(), old.status(), old.createdAt(), Instant.now(), old.version()));
        }).map(this::view);
    }

    @Override public Mono<PlanningProjectView> changeStatus(UUID ownerId, UUID projectId, PlanningProjectStatus status) {
        return changeStatus(ownerId, projectId, status, null);
    }

    @Override public Mono<PlanningProjectView> changeStatus(UUID ownerId, UUID projectId, PlanningProjectStatus status,
                                                              long expectedVersion) {
        return changeStatus(ownerId, projectId, status, Long.valueOf(expectedVersion));
    }

    private Mono<PlanningProjectView> changeStatus(UUID ownerId, UUID projectId, PlanningProjectStatus status,
                                                    Long expectedVersion) {
        return owned(ownerId, projectId).flatMap(old -> {
            if (expectedVersion != null && (old.version() == null ? 0 : old.version()) != expectedVersion) {
                return Mono.error(new PreconditionFailedException("If-Match 与 Project 当前版本不匹配"));
            }
            if (old.status() == PlanningProjectStatus.ARCHIVED && status != PlanningProjectStatus.ARCHIVED) {
                return Mono.error(new ConflictException("已归档项目不能恢复"));
            }
            return repository.save(new PlanningProjectEntity(old.id(), old.ownerId(), old.name(), old.description(),
                status, old.createdAt(), Instant.now(), old.version()));
        }).map(this::view);
    }

    private Mono<PlanningProjectEntity> owned(UUID ownerId, UUID projectId) {
        return repository.findById(projectId).filter(project -> project.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Project 不存在")));
    }

    private void check(PlanningProjectEntity project, Long expectedVersion) {
        if (expectedVersion != null && (project.version() == null ? 0 : project.version()) != expectedVersion) {
            throw new ConflictException("Project 版本冲突");
        }
    }

    private PlanningProjectView view(PlanningProjectEntity project) {
        return new PlanningProjectView(project.id(), project.ownerId(), project.name(), project.description(),
            project.status(), project.createdAt(), project.updatedAt(), project.version() == null ? 0 : project.version());
    }
}
