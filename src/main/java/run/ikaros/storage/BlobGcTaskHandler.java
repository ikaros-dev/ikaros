package run.ikaros.storage;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.task.BackgroundTask;
import run.ikaros.task.BackgroundTaskDispatcher;
import run.ikaros.event.DurableEventService;

/** storage.blob-gc 的受控执行 Handler；执行时重新判断引用，避免 TOCTOU 删除。 */
@Component
public class BlobGcTaskHandler {
    private final BackgroundTaskDispatcher dispatcher;
    private final StorageService storage;
    private final BlobGarbageCollector collector;
    private final DurableEventService events;

    public BlobGcTaskHandler(BackgroundTaskDispatcher dispatcher, StorageService storage,
                             BlobGarbageCollector collector, DurableEventService events) {
        this.dispatcher = dispatcher; this.storage = storage; this.collector = collector; this.events = events;
    }

    @PostConstruct
    void register() {
        dispatcher.register("storage.blob-gc", this::handle);
    }

    private Mono<Map<String, Object>> handle(BackgroundTask task) {
        int limit = (int) number(task.payload().get("limit"), 100);
        long age = number(task.payload().get("minimum_age_seconds"), 86400L);
        return storage.findGarbageCollectionCandidates(limit, Duration.ofSeconds(age))
            .flatMapMany(reactor.core.publisher.Flux::fromIterable)
            .flatMap(candidate -> {
                String requested = "{\"blob_id\":\"" + candidate.blobId() + "\",\"task_id\":\"" + task.id() + "\"}";
                return events.append("storage.blob.gc-requested", 1, "blob", candidate.blobId(), requested)
                    .then(collector.purge(candidate.blobId()))
                    .then(events.append("storage.blob.purged", 1, "blob", candidate.blobId(),
                        "{\"blob_id\":\"" + candidate.blobId() + "\",\"purged_placement_count\":1}"))
                    .thenReturn(candidate.blobId());
            })
            .collectList()
            .map(purged -> {
                Map<String, Object> result = new HashMap<>();
                result.put("purged_count", purged.size());
                result.put("blob_ids", purged.stream().map(Object::toString).toList());
                return result;
            });
    }

    private long number(Object value, long fallback) {
        return value instanceof Number number ? number.longValue() : fallback;
    }
}
