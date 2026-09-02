package run.ikaros.planning;
import java.time.Instant;
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
    @Override public Mono<PlanningTaskView> create(UUID owner,CreatePlanningTaskRequest req){validateSchedule(req.scheduledStart(),req.scheduledEnd());Instant now=Instant.now();return repository.save(new PlanningTaskEntity(null,owner,req.title().trim(),req.description(),PlanningTaskStatus.INBOX,req.priority()==null?PlanningTaskPriority.NONE:req.priority(),req.scheduledStart(),req.scheduledEnd(),req.deadline(),req.projectId(),req.parentTaskId(),null,now,now,null)).map(this::view);}
    @Override public Flux<PlanningTaskView> list(UUID owner,PlanningTaskStatus status){return (status==null?repository.findAllByOwnerIdOrderByCreatedAtDesc(owner):repository.findAllByOwnerIdAndStatusOrderByCreatedAtDesc(owner,status)).map(this::view);}
    @Override public Mono<PlanningTaskView> update(UUID owner,UUID id,UpdatePlanningTaskRequest req){return owned(owner,id).flatMap(old->{check(old,req.expectedVersion());validateSchedule(req.scheduledStart(),req.scheduledEnd());return repository.save(new PlanningTaskEntity(old.id(),old.ownerId(),req.title().trim(),req.description(),old.status(),req.priority()==null?PlanningTaskPriority.NONE:req.priority(),req.scheduledStart(),req.scheduledEnd(),req.deadline(),old.projectId(),old.parentTaskId(),old.completedAt(),old.createdAt(),Instant.now(),old.version()));}).map(this::view);}
    @Override public Mono<PlanningTaskView> changeStatus(UUID owner,UUID id,PlanningTaskStatus status){return owned(owner,id).flatMap(old->{if(old.status()==PlanningTaskStatus.ARCHIVED)return Mono.error(new ConflictException("已归档任务不能修改"));Instant completed=status==PlanningTaskStatus.COMPLETED?Instant.now():null;return repository.save(new PlanningTaskEntity(old.id(),old.ownerId(),old.title(),old.description(),status,old.priority(),old.scheduledStart(),old.scheduledEnd(),old.deadline(),old.projectId(),old.parentTaskId(),completed,old.createdAt(),Instant.now(),old.version()));}).map(this::view);}
    private Mono<PlanningTaskEntity> owned(UUID owner,UUID id){return repository.findById(id).filter(t->t.ownerId().equals(owner)).switchIfEmpty(Mono.error(new NotFoundException("Task 不存在")));}
    private void check(PlanningTaskEntity t,long expected){long actual=t.version()==null?0:t.version();if(actual!=expected)throw new ConflictException("Task 版本冲突");}
    private void validateSchedule(Instant start, Instant end){if(start!=null&&end!=null&&!end.isAfter(start))throw new ConflictException("计划结束时间必须晚于开始时间");}
    private PlanningTaskView view(PlanningTaskEntity t){return new PlanningTaskView(t.id(),t.ownerId(),t.title(),t.description(),t.status(),t.priority(),t.scheduledStart(),t.scheduledEnd(),t.deadline(),t.projectId(),t.parentTaskId(),t.completedAt(),t.createdAt(),t.updatedAt(),t.version()==null?0:t.version());}
}
