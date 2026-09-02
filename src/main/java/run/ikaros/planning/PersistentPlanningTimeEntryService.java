package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentPlanningTimeEntryService implements PlanningTimeEntryService {
    private final PlanningTimeEntryRepository entries;
    private final PlanningTaskRepository tasks;
    public PersistentPlanningTimeEntryService(PlanningTimeEntryRepository entries, PlanningTaskRepository tasks) { this.entries = entries; this.tasks = tasks; }
    @Override public Mono<PlanningTimeEntryView> create(UUID ownerId, UUID taskId, CreatePlanningTimeEntryRequest request) {
        return task(ownerId, taskId).then(Mono.defer(() -> {
            if (request.startedAt() != null && request.endedAt() != null && !request.endedAt().isAfter(request.startedAt())) return Mono.error(new ConflictException("实际耗时结束时间必须晚于开始时间"));
            Instant now = Instant.now();
            return entries.save(new PlanningTimeEntryEntity(null, ownerId, taskId, request.durationMinutes(), request.startedAt(), request.endedAt(),
                request.source() == null ? PlanningTimeEntrySource.MANUAL : request.source(), request.note(), now)).map(this::view);
        }));
    }
    @Override public Flux<PlanningTimeEntryView> list(UUID ownerId, UUID taskId) { return task(ownerId, taskId).thenMany(entries.findAllByOwnerIdAndTaskIdOrderByCreatedAtDesc(ownerId, taskId)).map(this::view); }
    private Mono<PlanningTaskEntity> task(UUID ownerId, UUID taskId) { return tasks.findById(taskId).filter(t -> t.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在"))); }
    private PlanningTimeEntryView view(PlanningTimeEntryEntity entry) { return new PlanningTimeEntryView(entry.id(), entry.ownerId(), entry.taskId(), entry.durationMinutes(), entry.startedAt(), entry.endedAt(), entry.source(), entry.note(), entry.createdAt()); }
}
