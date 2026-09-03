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
public class PersistentPlanningReviewService implements PlanningReviewService {
    private final PlanningReviewRepository reviews;
    public PersistentPlanningReviewService(PlanningReviewRepository reviews) { this.reviews = reviews; }
    @Override public Mono<PlanningReviewView> create(UUID ownerId, CreatePlanningReviewRequest request) {
        validate(request.periodStart(), request.periodEnd());
        return reviews.findByOwnerIdAndPeriodAndPeriodStart(ownerId, request.period(), request.periodStart())
            .flatMap(existing -> Mono.<PlanningReviewView>error(new ConflictException("该周期的 Review 已存在")))
            .switchIfEmpty(Mono.defer(() -> { Instant now = Instant.now(); return reviews.save(new PlanningReviewEntity(null, ownerId, request.period(), request.periodStart(), request.periodEnd(), request.note().trim(), request.wins(), request.challenges(), request.nextFocus(), now, now, null)).map(this::view); }));
    }
    @Override public Flux<PlanningReviewView> list(UUID ownerId, PlanningReviewPeriod period) { Flux<PlanningReviewEntity> source = reviews.findAllByOwnerIdOrderByPeriodStartDesc(ownerId); return (period == null ? source : source.filter(r -> r.period() == period)).take(100).map(this::view); }
    @Override public Mono<PlanningReviewView> update(UUID ownerId, UUID reviewId, UpdatePlanningReviewRequest request) { validate(request.periodStart(), request.periodEnd()); return owned(ownerId, reviewId).flatMap(old -> { if ((old.version() == null ? 0 : old.version()) != request.expectedVersion()) return Mono.error(new PreconditionFailedException("If-Match 与 Review 当前版本不匹配")); return reviews.save(new PlanningReviewEntity(old.id(), old.ownerId(), old.period(), request.periodStart(), request.periodEnd(), request.note().trim(), request.wins(), request.challenges(), request.nextFocus(), old.createdAt(), Instant.now(), old.version())); }).map(this::view); }
    private Mono<PlanningReviewEntity> owned(UUID ownerId, UUID id) { return reviews.findById(id).filter(r -> r.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Review 不存在"))); }
    private void validate(Instant start, Instant end) { if (!end.isAfter(start)) throw new ConflictException("Review 结束时间必须晚于开始时间"); }
    private PlanningReviewView view(PlanningReviewEntity r) { return new PlanningReviewView(r.id(), r.ownerId(), r.period(), r.periodStart(), r.periodEnd(), r.note(), r.wins(), r.challenges(), r.nextFocus(), r.createdAt(), r.updatedAt(), r.version() == null ? 0 : r.version()); }
}
