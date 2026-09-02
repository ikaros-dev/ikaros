package run.ikaros.planning;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
@Service
public class PersistentPlanningTaskService implements PlanningTaskService {
    private final PlanningTaskRepository repository;
    public PersistentPlanningTaskService(PlanningTaskRepository repository){this.repository=repository;}
    @Override public Mono<PlanningTaskView> create(UUID owner,CreatePlanningTaskRequest req){validateSchedule(req.scheduledStart(),req.scheduledEnd());validateDuration(req.estimatedDurationMinutes());Instant now=Instant.now();return repository.save(new PlanningTaskEntity(null,owner,req.title().trim(),req.description(),PlanningTaskStatus.INBOX,req.priority()==null?PlanningTaskPriority.NONE:req.priority(),req.important(),req.urgent(),req.scheduledStart(),req.scheduledEnd(),req.deadline(),req.estimatedDurationMinutes(),req.projectId(),req.parentTaskId(),null,now,now,null)).map(this::view);}
    @Override public Flux<PlanningTaskView> list(UUID owner,PlanningTaskStatus status){return (status==null?repository.findAllByOwnerIdOrderByCreatedAtDesc(owner):repository.findAllByOwnerIdAndStatusOrderByCreatedAtDesc(owner,status)).map(this::view);}
    @Override public Flux<PlanningTaskView> today(UUID owner, ZoneId zoneId){ZonedDateTime start=Instant.now().atZone(zoneId).toLocalDate().atStartOfDay(zoneId);Instant from=start.toInstant(),to=start.plusDays(1).toInstant();return repository.findAllByOwnerIdOrderByCreatedAtDesc(owner).filter(t->visible(t)&&inWindow(t,from,to)).map(this::view);}
    @Override public Flux<PlanningTaskView> upcoming(UUID owner, Instant from){Instant now=from==null?Instant.now():from;return repository.findAllByOwnerIdOrderByCreatedAtDesc(owner).filter(t->visible(t)&&((t.scheduledStart()!=null&&!t.scheduledStart().isBefore(now))||(t.deadline()!=null&&!t.deadline().isBefore(now)))).map(this::view);}
    @Override public Flux<PlanningTaskView> filter(UUID owner, PlanningTaskStatus status, PlanningTaskPriority priority, Instant from, Instant to, boolean overdue){Instant now=Instant.now();return repository.findAllByOwnerIdOrderByCreatedAtDesc(owner).filter(t->(status==null||t.status()==status)&&(priority==null||t.priority()==priority)&&(from==null||t.deadline()==null||!t.deadline().isBefore(from))&&(to==null||t.deadline()==null||t.deadline().isBefore(to))&&(!overdue||t.deadline()!=null&&t.deadline().isBefore(now)&&visible(t))).map(this::view);}
    @Override public Flux<PlanningTaskView> eisenhower(UUID owner, boolean important, boolean urgent){return repository.findAllByOwnerIdOrderByCreatedAtDesc(owner).filter(t->visible(t)&&t.important()==important&&t.urgent()==urgent).map(this::view);}
    @Override public Mono<PlanningTaskView> update(UUID owner,UUID id,UpdatePlanningTaskRequest req){return owned(owner,id).flatMap(old->{check(old,req.expectedVersion());validateSchedule(req.scheduledStart(),req.scheduledEnd());validateDuration(req.estimatedDurationMinutes());return repository.save(new PlanningTaskEntity(old.id(),old.ownerId(),req.title().trim(),req.description(),old.status(),req.priority()==null?PlanningTaskPriority.NONE:req.priority(),req.important(),req.urgent(),req.scheduledStart(),req.scheduledEnd(),req.deadline(),req.estimatedDurationMinutes(),old.projectId(),old.parentTaskId(),old.completedAt(),old.createdAt(),Instant.now(),old.version()));}).map(this::view);}
    @Override public Mono<PlanningTaskView> changeStatus(UUID owner,UUID id,PlanningTaskStatus status){return owned(owner,id).flatMap(old->{if(old.status()==PlanningTaskStatus.ARCHIVED)return Mono.error(new ConflictException("已归档任务不能修改"));Instant completed=status==PlanningTaskStatus.COMPLETED?Instant.now():null;return repository.save(new PlanningTaskEntity(old.id(),old.ownerId(),old.title(),old.description(),status,old.priority(),old.important(),old.urgent(),old.scheduledStart(),old.scheduledEnd(),old.deadline(),old.estimatedDurationMinutes(),old.projectId(),old.parentTaskId(),completed,old.createdAt(),Instant.now(),old.version()));}).map(this::view);}
    private Mono<PlanningTaskEntity> owned(UUID owner,UUID id){return repository.findById(id).filter(t->t.ownerId().equals(owner)).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")));}
    private void check(PlanningTaskEntity t,long expected){long actual=t.version()==null?0:t.version();if(actual!=expected)throw new ConflictException("Task 版本冲突");}
    private void validateSchedule(Instant start, Instant end){if(start!=null&&end!=null&&!end.isAfter(start))throw new ConflictException("计划结束时间必须晚于开始时间");}
    private void validateDuration(Integer minutes){if(minutes!=null&&minutes<=0)throw new ConflictException("预计时长必须大于 0");}
    private boolean visible(PlanningTaskEntity t){return t.status()!=PlanningTaskStatus.COMPLETED&&t.status()!=PlanningTaskStatus.CANCELLED&&t.status()!=PlanningTaskStatus.ARCHIVED;}
    private boolean inWindow(PlanningTaskEntity t,Instant from,Instant to){return (t.scheduledStart()!=null&&!t.scheduledStart().isBefore(from)&&t.scheduledStart().isBefore(to))||(t.deadline()!=null&&!t.deadline().isBefore(from)&&t.deadline().isBefore(to));}
    private PlanningTaskView view(PlanningTaskEntity t){return new PlanningTaskView(t.id(),t.ownerId(),t.title(),t.description(),t.status(),t.priority(),t.important(),t.urgent(),t.scheduledStart(),t.scheduledEnd(),t.deadline(),t.estimatedDurationMinutes(),t.projectId(),t.parentTaskId(),t.completedAt(),t.createdAt(),t.updatedAt(),t.version()==null?0:t.version());}
}
