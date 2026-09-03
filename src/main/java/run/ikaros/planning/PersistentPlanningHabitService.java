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
public class PersistentPlanningHabitService implements PlanningHabitService {
    private final PlanningHabitRepository habits;
    private final PlanningHabitCheckInRepository checkIns;
    public PersistentPlanningHabitService(PlanningHabitRepository habits, PlanningHabitCheckInRepository checkIns) { this.habits = habits; this.checkIns = checkIns; }
    @Override public Mono<PlanningHabitView> create(UUID ownerId, CreatePlanningHabitRequest request) {
        if (request.targetValue() != null && request.targetValue() <= 0) return Mono.error(new ConflictException("习惯目标值必须大于 0"));
        Instant now = Instant.now();
        return habits.save(new PlanningHabitEntity(null, ownerId, request.name().trim(), request.description(), request.metric() == null ? PlanningHabitMetric.BOOLEAN : request.metric(),
            request.targetValue(), request.schedule().trim(), request.timeZone() == null ? "UTC" : request.timeZone(), request.startAt(), PlanningHabitStatus.ACTIVE, now, now, null)).map(this::view);
    }
    @Override public Flux<PlanningHabitView> list(UUID ownerId) { return habits.findAllByOwnerIdOrderByCreatedAtDesc(ownerId).take(100).map(this::view); }
    @Override public Mono<PlanningHabitView> archive(UUID ownerId, UUID habitId) { return archiveInternal(ownerId, habitId, null); }
    @Override public Mono<PlanningHabitView> archive(UUID ownerId, UUID habitId, long expectedVersion) { return archiveInternal(ownerId, habitId, expectedVersion); }
    private Mono<PlanningHabitView> archiveInternal(UUID ownerId, UUID habitId, Long expectedVersion) { return owned(ownerId, habitId).flatMap(old -> { if (expectedVersion != null && (old.version() == null ? 0 : old.version()) != expectedVersion) return Mono.error(new PreconditionFailedException("If-Match 与 Habit 当前版本不匹配")); return habits.save(new PlanningHabitEntity(old.id(), old.ownerId(), old.name(), old.description(), old.metric(), old.targetValue(), old.schedule(), old.timeZone(), old.startAt(), PlanningHabitStatus.ARCHIVED, old.createdAt(), Instant.now(), old.version())); }).map(this::view); }
    @Override public Mono<PlanningHabitCheckInView> checkIn(UUID ownerId, UUID habitId, CreatePlanningHabitCheckInRequest request) { return owned(ownerId, habitId).flatMap(habit -> { if (habit.status() == PlanningHabitStatus.ARCHIVED) return Mono.error(new ConflictException("已归档习惯不能打卡"));
        Instant occurred = request.occurredAt() == null ? Instant.now() : request.occurredAt(); return checkIns.save(new PlanningHabitCheckInEntity(null, ownerId, habitId, request.value(), occurred, request.note(), Instant.now())).map(this::checkInView); }); }
    @Override public Flux<PlanningHabitCheckInView> listCheckIns(UUID ownerId, UUID habitId) { return owned(ownerId, habitId).thenMany(checkIns.findAllByOwnerIdAndHabitIdOrderByOccurredAtDesc(ownerId, habitId).take(100)).map(this::checkInView); }
    private Mono<PlanningHabitEntity> owned(UUID ownerId, UUID id) { return habits.findById(id).filter(habit -> habit.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Habit 不存在"))); }
    private PlanningHabitView view(PlanningHabitEntity habit) { return new PlanningHabitView(habit.id(), habit.ownerId(), habit.name(), habit.description(), habit.metric(), habit.targetValue(), habit.schedule(), habit.timeZone(), habit.startAt(), habit.status(), habit.createdAt(), habit.updatedAt(), habit.version() == null ? 0 : habit.version()); }
    private PlanningHabitCheckInView checkInView(PlanningHabitCheckInEntity checkIn) { return new PlanningHabitCheckInView(checkIn.id(), checkIn.ownerId(), checkIn.habitId(), checkIn.value(), checkIn.occurredAt(), checkIn.note(), checkIn.createdAt()); }
}
