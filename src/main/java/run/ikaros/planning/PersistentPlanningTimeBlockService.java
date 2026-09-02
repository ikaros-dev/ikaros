package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentPlanningTimeBlockService implements PlanningTimeBlockService {
    private final PlanningTimeBlockRepository blocks;
    private final PlanningTaskRepository tasks;
    private final PlanningProjectRepository projects;
    private final PlanningProjectMemberRepository members;

    public PersistentPlanningTimeBlockService(PlanningTimeBlockRepository blocks, PlanningTaskRepository tasks,
        PlanningProjectRepository projects, PlanningProjectMemberRepository members) {
        this.blocks = blocks; this.tasks = tasks; this.projects = projects; this.members = members;
    }

    @Override public Mono<PlanningTimeBlockView> create(UUID ownerId, CreatePlanningTimeBlockRequest request) {
        validateRange(request.startAt(), request.endAt());
        return task(ownerId, request.taskId()).then(overlap(ownerId, request.startAt(), request.endAt(), null)
            .flatMap(conflict -> Mono.<PlanningTimeBlockView>error(new ConflictException("Time Block 与已有安排重叠")))
            .switchIfEmpty(Mono.defer(() -> {
                Instant now = Instant.now();
                return blocks.save(new PlanningTimeBlockEntity(null, ownerId, request.title().trim(), request.taskId(),
                    request.startAt(), request.endAt(), request.kind() == null ? PlanningTimeBlockKind.FIXED : request.kind(),
                    PlanningTimeBlockStatus.ACTIVE, request.timeZone() == null ? "UTC" : request.timeZone(), now, now, null)).map(this::view);
            })));
    }

    @Override public Flux<PlanningTimeBlockView> list(UUID ownerId, Instant from, Instant to) {
        Flux<PlanningTimeBlockEntity> source = from == null || to == null ? blocks.findAllByOwnerIdOrderByStartAt(ownerId)
            : blocks.findAllByOwnerIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAt(ownerId, to, from);
        return source.filter(block -> block.status() == PlanningTimeBlockStatus.ACTIVE).map(this::view);
    }

    @Override public Mono<PlanningTimeBlockView> update(UUID ownerId, UUID blockId, UpdatePlanningTimeBlockRequest request) {
        validateRange(request.startAt(), request.endAt());
        return owned(ownerId, blockId).flatMap(old -> {
            if (old.status() == PlanningTimeBlockStatus.CANCELLED) return Mono.error(new ConflictException("已取消的 Time Block 不能修改"));
            if ((old.version() == null ? 0 : old.version()) != request.expectedVersion()) return Mono.error(new ConflictException("Time Block 版本冲突"));
            return overlap(ownerId, request.startAt(), request.endAt(), blockId).flatMap(conflict -> Mono.<PlanningTimeBlockEntity>error(new ConflictException("Time Block 与已有安排重叠")))
                .switchIfEmpty(Mono.defer(() -> blocks.save(new PlanningTimeBlockEntity(old.id(), old.ownerId(), old.title(), old.taskId(),
                    request.startAt(), request.endAt(), request.kind() == null ? old.kind() : request.kind(), old.status(),
                    request.timeZone() == null ? old.timeZone() : request.timeZone(), old.createdAt(), Instant.now(), old.version()))));
        }).map(this::view);
    }

    @Override public Mono<PlanningTimeBlockView> cancel(UUID ownerId, UUID blockId) {
        return owned(ownerId, blockId).flatMap(old -> blocks.save(new PlanningTimeBlockEntity(old.id(), old.ownerId(), old.title(), old.taskId(),
            old.startAt(), old.endAt(), old.kind(), PlanningTimeBlockStatus.CANCELLED, old.timeZone(), old.createdAt(), Instant.now(), old.version()))).map(this::view);
    }

    private Mono<PlanningTaskEntity> task(UUID ownerId, UUID taskId) {
        if (taskId == null) return Mono.empty();
        return tasks.findById(taskId).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在"))).flatMap(task -> {
            if (task.ownerId().equals(ownerId)) return Mono.just(task);
            if (task.projectId() == null) return Mono.error(new NotFoundException("Task 不存在或无权访问"));
            return projects.findById(task.projectId()).flatMap(project -> project.ownerId().equals(ownerId)
                ? Mono.just(task) : members.findByProjectIdAndUserId(task.projectId(), ownerId).map(member -> task))
                .switchIfEmpty(Mono.error(new NotFoundException("Task 不存在或无权访问")));
        });
    }
    private Mono<PlanningTimeBlockEntity> owned(UUID ownerId, UUID blockId) { return blocks.findById(blockId)
        .filter(block -> block.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Time Block 不存在"))); }
    private Mono<PlanningTimeBlockEntity> overlap(UUID ownerId, Instant startAt, Instant endAt, UUID excludedId) {
        return blocks.findAllByOwnerIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAt(ownerId, endAt, startAt)
            .filter(block -> excludedId == null || !block.id().equals(excludedId)).next();
    }
    private void validateRange(Instant startAt, Instant endAt) { if (!endAt.isAfter(startAt)) throw new ConflictException("Time Block 结束时间必须晚于开始时间"); }
    private PlanningTimeBlockView view(PlanningTimeBlockEntity block) { return new PlanningTimeBlockView(block.id(), block.ownerId(), block.title(),
        block.taskId(), block.startAt(), block.endAt(), block.kind(), block.status(), block.timeZone(), block.createdAt(), block.updatedAt(), block.version() == null ? 0 : block.version()); }
}
