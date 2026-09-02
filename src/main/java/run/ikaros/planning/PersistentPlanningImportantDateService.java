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
public class PersistentPlanningImportantDateService implements PlanningImportantDateService {
    private final PlanningImportantDateRepository dates;

    public PersistentPlanningImportantDateService(PlanningImportantDateRepository dates) {
        this.dates = dates;
    }

    public Mono<PlanningImportantDateView> create(UUID owner, CreatePlanningImportantDateRequest request) {
        Instant now = Instant.now();
        return dates.save(new PlanningImportantDateEntity(null, owner, request.title().trim(), request.description(),
            request.occursAt(), request.timeZone() == null ? "UTC" : request.timeZone(), request.kind(),
            PlanningImportantDateStatus.ACTIVE, now, now, null)).map(this::view);
    }

    public Flux<PlanningImportantDateView> list(UUID owner) {
        return dates.findAllByOwnerIdOrderByOccursAtAsc(owner).map(this::view);
    }

    public Mono<PlanningImportantDateView> update(UUID owner, UUID id, UpdatePlanningImportantDateRequest request) {
        return owned(owner, id).flatMap(old -> {
            if (old.status() == PlanningImportantDateStatus.ARCHIVED) {
                return Mono.error(new ConflictException("已归档日期不能修改"));
            }
            long actual = old.version() == null ? 0 : old.version();
            if (actual != request.expectedVersion()) {
                return Mono.error(new PreconditionFailedException("If-Match 与 Important Date 当前版本不匹配"));
            }
            return dates.save(new PlanningImportantDateEntity(old.id(), old.ownerId(), request.title().trim(),
                request.description(), request.occursAt(), request.timeZone() == null ? old.timeZone() : request.timeZone(),
                request.kind(), old.status(), old.createdAt(), Instant.now(), old.version()));
        }).map(this::view);
    }

    public Mono<PlanningImportantDateView> archive(UUID owner, UUID id) { return archiveInternal(owner, id, null); }

    public Mono<PlanningImportantDateView> archive(UUID owner, UUID id, long expectedVersion) { return archiveInternal(owner, id, expectedVersion); }

    private Mono<PlanningImportantDateView> archiveInternal(UUID owner, UUID id, Long expectedVersion) {
        return owned(owner, id).flatMap(old -> { if (expectedVersion != null && (old.version() == null ? 0 : old.version()) != expectedVersion) return Mono.error(new PreconditionFailedException("If-Match 与 Important Date 当前版本不匹配")); return dates.save(new PlanningImportantDateEntity(old.id(), old.ownerId(),
            old.title(), old.description(), old.occursAt(), old.timeZone(), old.kind(), PlanningImportantDateStatus.ARCHIVED,
            old.createdAt(), Instant.now(), old.version())); }).map(this::view);
    }

    private Mono<PlanningImportantDateEntity> owned(UUID owner, UUID id) {
        return dates.findById(id).filter(date -> date.ownerId().equals(owner))
            .switchIfEmpty(Mono.error(new NotFoundException("Important Date 不存在")));
    }

    private PlanningImportantDateView view(PlanningImportantDateEntity date) {
        return new PlanningImportantDateView(date.id(), date.ownerId(), date.title(), date.description(), date.occursAt(),
            date.timeZone(), date.kind(), date.status(), date.createdAt(), date.updatedAt(), date.version() == null ? 0 : date.version());
    }
}
