package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class PersistentPlanningCalendarService implements PlanningCalendarService {
    private final PlanningTaskRepository tasks;
    private final PlanningTimeBlockRepository blocks;
    private final PlanningReminderRepository reminders;
    private final PlanningImportantDateRepository importantDates;
    private final PlanningProjectMemberRepository members;

    public PersistentPlanningCalendarService(PlanningTaskRepository tasks, PlanningTimeBlockRepository blocks,
        PlanningReminderRepository reminders, PlanningImportantDateRepository importantDates,
        PlanningProjectMemberRepository members) { this.tasks = tasks; this.blocks = blocks; this.reminders = reminders;
        this.importantDates = importantDates; this.members = members; }

    @Override public Flux<PlanningCalendarItemView> list(UUID ownerId, Instant from, Instant to) {
        if (from == null || to == null || !to.isAfter(from)) return Flux.error(new IllegalArgumentException("Calendar 时间范围无效"));
        Flux<PlanningCalendarItemView> scheduled = accessibleTasks(ownerId)
            .filter(task -> task.status() != PlanningTaskStatus.CANCELLED && task.status() != PlanningTaskStatus.ARCHIVED
                && task.scheduledStart() != null && task.scheduledStart().isBefore(to)
                && (task.scheduledEnd() == null || task.scheduledEnd().isAfter(from)))
            .map(task -> new PlanningCalendarItemView(PlanningCalendarItemType.SCHEDULED_TASK, task.id(), task.id(), task.title(),
                task.scheduledStart(), task.scheduledEnd() == null ? task.scheduledStart() : task.scheduledEnd(), "UTC"));
        Flux<PlanningCalendarItemView> deadlines = accessibleTasks(ownerId)
            .filter(task -> task.status() != PlanningTaskStatus.CANCELLED && task.status() != PlanningTaskStatus.ARCHIVED
                && task.deadline() != null && !task.deadline().isBefore(from) && task.deadline().isBefore(to))
            .map(task -> new PlanningCalendarItemView(PlanningCalendarItemType.TASK_DEADLINE, task.id(), task.id(), task.title(), task.deadline(), task.deadline(), "UTC"));
        Flux<PlanningCalendarItemView> timeBlocks = blocks.findAllByOwnerIdAndStartAtLessThanAndEndAtGreaterThanOrderByStartAt(ownerId, to, from)
            .filter(block -> block.status() == PlanningTimeBlockStatus.ACTIVE)
            .map(block -> new PlanningCalendarItemView(PlanningCalendarItemType.TIME_BLOCK, block.id(), block.id(), block.title(), block.startAt(), block.endAt(), block.timeZone()));
        Flux<PlanningCalendarItemView> reminderItems = reminders.findAllByOwnerIdOrderByTriggerAt(ownerId)
            .filter(reminder -> reminder.status() != PlanningReminderStatus.CANCELLED && reminder.triggerAt() != null
                && !reminder.triggerAt().isBefore(from) && reminder.triggerAt().isBefore(to))
            .map(reminder -> new PlanningCalendarItemView(PlanningCalendarItemType.REMINDER, reminder.id(), reminder.targetId(),
                "Reminder", reminder.triggerAt(), reminder.triggerAt(), reminder.timeZone()));
        Flux<PlanningCalendarItemView> dateItems = importantDates.findAllByOwnerIdOrderByOccursAtAsc(ownerId)
            .filter(date -> date.status() == PlanningImportantDateStatus.ACTIVE && !date.occursAt().isBefore(from) && date.occursAt().isBefore(to))
            .map(date -> new PlanningCalendarItemView(PlanningCalendarItemType.IMPORTANT_DATE, date.id(), date.id(), date.title(), date.occursAt(), date.occursAt(), date.timeZone()));
        return Flux.merge(scheduled, deadlines, timeBlocks, reminderItems, dateItems).sort(java.util.Comparator.comparing(PlanningCalendarItemView::startAt));
    }

    private Flux<PlanningTaskEntity> accessibleTasks(UUID ownerId) {
        Flux<PlanningTaskEntity> owned = tasks.findAllByOwnerIdOrderByCreatedAtDesc(ownerId);
        Flux<PlanningTaskEntity> shared = members.findAllByUserId(ownerId)
            .map(PlanningProjectMemberEntity::projectId)
            .flatMap(tasks::findAllByProjectIdOrderByCreatedAtDesc);
        return Flux.concat(owned, shared).distinct(PlanningTaskEntity::id);
    }
}
