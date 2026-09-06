package run.ikaros.storage;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.r2dbc.repository.Query;
import reactor.core.publisher.Mono;

public interface StorageRestoreBudgetRepository extends ReactiveCrudRepository<StorageRestoreBudgetEntity, UUID> {
    @Query("select count(*) from storage_restore_request where status in ('REQUESTED', 'IN_PROGRESS')")
    Mono<Long> countActiveRequests();

    @Query("select coalesce(sum(total_bytes), 0) from storage_restore_request where status in ('REQUESTED', 'IN_PROGRESS')")
    Mono<Long> sumActiveBytes();

    @Query("select coalesce(sum(total_bytes), 0) from storage_restore_request where created_at >= :startAt")
    Mono<Long> sumRequestedBytesSince(Instant startAt);
}
