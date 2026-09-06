package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PersistentPlanningStatisticsService implements PlanningStatisticsService {
    private final PlanningTaskRepository tasks;
    private final PlanningTimeEntryRepository entries;
    private final PlanningGoalRepository goals;
    private final PlanningHabitCheckInRepository checkIns;
    public PersistentPlanningStatisticsService(PlanningTaskRepository tasks, PlanningTimeEntryRepository entries,
        PlanningGoalRepository goals, PlanningHabitCheckInRepository checkIns) { this.tasks = tasks; this.entries = entries; this.goals = goals; this.checkIns = checkIns; }
    @Override public Mono<PlanningStatisticsView> summarize(UUID ownerId, Instant from, Instant to) {
        if (from == null || to == null || !to.isAfter(from)) return Mono.error(new IllegalArgumentException("Statistics 时间范围无效"));
        return Mono.zip(tasks.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).filter(t -> !t.createdAt().isBefore(from) && t.createdAt().isBefore(to)).collectList(),
            entries.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).filter(e -> !e.createdAt().isBefore(from) && e.createdAt().isBefore(to)).collectList(),
            goals.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId).filter(g -> !g.createdAt().isBefore(from) && g.createdAt().isBefore(to)).count(),
            checkIns.findAllByOwnerIdOrderByOccurredAtDesc(ownerId).filter(c -> !c.occurredAt().isBefore(from) && c.occurredAt().isBefore(to)).count())
            .map(tuple -> { var taskList = tuple.getT1(); var entryList = tuple.getT2(); long estimated = taskList.stream().filter(t -> t.estimatedDurationMinutes() != null).mapToLong(t -> t.estimatedDurationMinutes()).sum();
                long tracked = entryList.stream().mapToLong(PlanningTimeEntryEntity::durationMinutes).sum(); long focus = entryList.stream().filter(e -> e.source() == PlanningTimeEntrySource.FOCUS).mapToLong(PlanningTimeEntryEntity::durationMinutes).sum();
                return new PlanningStatisticsView(from, to, taskList.size(), taskList.stream().filter(t -> t.status() == PlanningTaskStatus.COMPLETED).count(), estimated, tracked, focus, tuple.getT3(), tuple.getT4()); });
    }
}
