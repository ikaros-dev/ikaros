package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentPlanningTagService implements PlanningTagService {
    private final PlanningTagRepository tags;
    private final PlanningTaskTagRepository taskTags;
    private final PlanningTaskRepository tasks;

    public PersistentPlanningTagService(PlanningTagRepository tags, PlanningTaskTagRepository taskTags,
        PlanningTaskRepository tasks) { this.tags = tags; this.taskTags = taskTags; this.tasks = tasks; }

    @Override public Mono<PlanningTagView> create(UUID ownerId, CreatePlanningTagRequest request) {
        String name = request.name().trim();
        return tags.findByOwnerIdAndNameIgnoreCase(ownerId, name)
            .flatMap(existing -> Mono.<PlanningTagView>error(new ConflictException("Tag 已存在")))
            .switchIfEmpty(tags.save(new PlanningTagEntity(null, ownerId, name, request.color(), Instant.now())).map(this::view));
    }

    @Override public Flux<PlanningTagView> list(UUID ownerId) { return tags.findAllByOwnerIdOrderByName(ownerId).map(this::view); }

    @Override public Mono<Void> attach(UUID ownerId, UUID taskId, UUID tagId) {
        return ownedTask(ownerId, taskId).then(ownedTag(ownerId, tagId)).then(taskTags.findByTaskIdAndTagId(taskId, tagId)
            .switchIfEmpty(taskTags.save(new PlanningTaskTagEntity(null, taskId, tagId, Instant.now())))
            .then()).then();
    }

    @Override public Mono<Void> detach(UUID ownerId, UUID taskId, UUID tagId) {
        return ownedTask(ownerId, taskId).then(ownedTag(ownerId, tagId)).then(taskTags.findByTaskIdAndTagId(taskId, tagId)
            .flatMap(taskTags::delete).then()).then();
    }

    @Override public Flux<PlanningTagView> listForTask(UUID ownerId, UUID taskId) {
        return ownedTask(ownerId, taskId).thenMany(taskTags.findAllByTaskId(taskId).flatMap(link -> ownedTag(ownerId, link.tagId()))).map(this::view);
    }

    private Mono<PlanningTaskEntity> ownedTask(UUID ownerId, UUID taskId) { return tasks.findById(taskId)
        .filter(task -> task.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在"))); }
    private Mono<PlanningTagEntity> ownedTag(UUID ownerId, UUID tagId) { return tags.findById(tagId)
        .filter(tag -> tag.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Tag 不存在"))); }
    private PlanningTagView view(PlanningTagEntity tag) { return new PlanningTagView(tag.id(), tag.ownerId(), tag.name(), tag.color(), tag.createdAt()); }
}
