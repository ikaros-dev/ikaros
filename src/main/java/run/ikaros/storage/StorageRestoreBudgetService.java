package run.ikaros.storage;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.PreconditionFailedException;
import run.ikaros.event.DurableEventService;

@Service
public class StorageRestoreBudgetService {
    public static final UUID DEFAULT_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final StorageRestoreBudgetRepository budgets;
    private final DurableEventService events;

    public StorageRestoreBudgetService(StorageRestoreBudgetRepository budgets, DurableEventService events) {
        this.budgets = budgets; this.events = events;
    }

    public Mono<StorageRestoreBudgetView> get() {
        return budgets.findById(DEFAULT_ID).map(this::view);
    }

    public Mono<StorageRestoreBudgetView> update(StorageRestoreBudgetRequest request) {
        return update(request, null);
    }

    public Mono<StorageRestoreBudgetView> update(StorageRestoreBudgetRequest request, Long expectedVersion) {
        Instant now = Instant.now();
        return budgets.findById(DEFAULT_ID).flatMap(old -> { if (expectedVersion != null && (old.version() == null ? 0 : old.version()) != expectedVersion)
            return Mono.error(new PreconditionFailedException("If-Match 与 Restore Budget 当前版本不匹配")); return budgets.save(new StorageRestoreBudgetEntity(DEFAULT_ID,
            request.maxBytesPerRequest(), request.maxItemsPerRequest(), request.maxConcurrentOperations(),
            request.maxConcurrentBytes(), request.dailyRequestedBytes(), request.dailyProviderRestoreBytes(),
            request.overBudgetAction(), now, old.version())); })
            .flatMap(saved -> events.append("storage.restore-budget.updated", 1, "restore_budget_policy", saved.id(),
                "{\"policy_id\":\"" + saved.id() + "\",\"scope_type\":\"INSTANCE\",\"scope_id\":null,\"version\":"
                    + (saved.version() == null ? 0 : saved.version()) + "}").thenReturn(view(saved)));
    }

    public Mono<Void> check(int items, long bytes) {
        return check(items, bytes, null);
    }

    public Mono<Void> check(int items, long bytes, String confirmationToken) {
        return evaluate(items, bytes, confirmationToken).then();
    }

    public Mono<StorageRestoreBudgetDecision> evaluate(int items, long bytes, String confirmationToken) {
        if (items < 1 || bytes < 0) return Mono.error(new IllegalArgumentException("Restore 请求规模无效"));
        return budgets.findById(DEFAULT_ID).flatMap(budget -> {
            if (items > budget.maxItemsPerRequest() || bytes > budget.maxBytesPerRequest())
                return overBudget(budget, confirmationToken, "Restore 请求超过单次预算");
            Instant startOfDay = ZonedDateTime.now(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            return budgets.countActiveRequests().zipWith(budgets.sumActiveBytes()).zipWith(budgets.sumRequestedBytesSince(startOfDay))
                .flatMap(values -> {
                    long activeCount = values.getT1().getT1();
                    long activeBytes = values.getT1().getT2();
                    long dailyBytes = values.getT2();
                    boolean exceedsConcurrentBytes = activeBytes > budget.maxConcurrentBytes()
                        || bytes > budget.maxConcurrentBytes() - activeBytes;
                    if (activeCount >= budget.maxConcurrentOperations() || exceedsConcurrentBytes
                        || dailyBytes > budget.dailyRequestedBytes() - bytes)
                        return overBudget(budget, confirmationToken, "Restore 请求超过并发或每日预算");
                    return Mono.just(StorageRestoreBudgetDecision.ACCEPTED);
                });
        });
    }

    private Mono<StorageRestoreBudgetDecision> overBudget(StorageRestoreBudgetEntity budget, String confirmationToken, String message) {
        if (budget.overBudgetAction() == StorageRestoreBudgetAction.REQUIRE_CONFIRMATION
            && confirmationToken != null && !confirmationToken.isBlank()) return Mono.just(StorageRestoreBudgetDecision.CONFIRMED);
        if (budget.overBudgetAction() == StorageRestoreBudgetAction.QUEUE_AFTER_BUDGET_RESET
            && message.contains("并发或每日")) return Mono.just(StorageRestoreBudgetDecision.QUEUED);
        return reject(budget, message).then(Mono.empty());
    }

    private Mono<Void> reject(StorageRestoreBudgetEntity budget, String message) {
        return switch (budget.overBudgetAction()) {
            case REJECT, REQUIRE_CONFIRMATION, QUEUE_AFTER_BUDGET_RESET, PARTIAL_ACCEPT ->
                Mono.error(new ConflictException("storage.restore.budget_exceeded", message));
        };
    }

    private StorageRestoreBudgetView view(StorageRestoreBudgetEntity b) {
        return new StorageRestoreBudgetView(b.id(), b.maxBytesPerRequest(), b.maxItemsPerRequest(),
            b.maxConcurrentOperations(), b.maxConcurrentBytes(), b.dailyRequestedBytes(),
            b.dailyProviderRestoreBytes(), b.overBudgetAction(), b.updatedAt(), b.version() == null ? 0 : b.version());
    }
}
