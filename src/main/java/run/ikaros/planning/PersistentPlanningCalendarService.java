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

    public PersistentPlanningCalendarService(PlanningTaskRepository tasks, PlanningTimeBlockRepository blocks,
        PlanningReminderRepository reminders) { this.tasks = tasks; this.blocks = blocks; this.reminders = reminders; }

    @Override public Flux<PlanningCalendarItemView> list(UUID ownerId, Instant from, Instant to) {
        if (from == null || to == null || !to.isAfter(from)) return Flux.error(new IllegalArgumentException("Calendar 时间范围无效"));
        Flux<PlanningCalendarItemView> scheduled = tasks.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)
            .filter(task -> task.status() != PlanningTaskStatus.CANCELLED && task.status() != PlanningTaskStatus.ARCHIVED
                && task.scheduledStart() != null && task.scheduledStart().isBefore(to)
                && (task.scheduledEnd() == null || task.scheduledEnd().isAfter(from)))
            .map(task -> new PlanningCalendarItemView(PlanningCalendarItemType.SCHEDULED_TASK, task.id(), task.id(), task.title(),
                task.scheduledStart(), task.scheduledEnd() == null ? task.scheduledStart() : task.scheduledEnd(), "UTC"));
        Flux<PlanningCalendarItemView> deadlines = tasks.findAllByOwnerIdOrderByCreatedAtDesc(ownerId)
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
        return Flux.merge(scheduled, deadlines, timeBlocks, reminderItems).sort(java.util.Comparator.comparing(PlanningCalendarItemView::startAt));
    }
}
