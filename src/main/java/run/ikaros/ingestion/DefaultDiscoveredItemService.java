package run.ikaros.ingestion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.event.DurableEventService;

@Service
public class DefaultDiscoveredItemService implements DiscoveredItemService {
    private static final int MAX_UNPAGED_RESULTS = 100;
    private final ScanRunRepository scanRuns;
    private final DiscoveredItemRepository items;
    private final DurableEventService events;

    public DefaultDiscoveredItemService(ScanRunRepository scanRuns, DiscoveredItemRepository items,
                                        DurableEventService events) {
        this.scanRuns = scanRuns;
        this.items = items;
        this.events = events;
    }

    @Override
    public Mono<DiscoveredItemView> record(UUID ownerId, UUID scanRunId, DiscoveredItemRequest request) {
        return scanRuns.findByIdAndOwnerId(scanRunId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("扫描运行不存在或无权访问")))
            .flatMap(run -> items.save(new DiscoveredItemEntity(null, run.sourceId(), scanRunId,
                request.relativeKey(), request.sizeBytes(), request.modifiedAt(), request.etag(), request.mediaType(),
                request.availability(), request.scanGeneration(), Instant.now(), null)))
            .map(this::view);
    }

    @Override
    public Mono<List<DiscoveredItemView>> list(UUID ownerId, UUID scanRunId) {
        return scanRuns.findByIdAndOwnerId(scanRunId, ownerId)
            .switchIfEmpty(Mono.error(new NotFoundException("扫描运行不存在或无权访问")))
            .thenMany(items.findAllByScanRunIdOrderByRelativeKeyAsc(scanRunId).take(MAX_UNPAGED_RESULTS))
            .map(this::view).collectList();
    }

    @Override
    public Mono<DiscoveredItemView> markUnavailable(UUID ownerId, UUID itemId, String reason) {
        return items.findById(itemId).switchIfEmpty(Mono.error(new NotFoundException("来源项目不存在")))
            .flatMap(item -> scanRuns.findByIdAndOwnerId(item.scanRunId(), ownerId)
                .switchIfEmpty(Mono.error(new NotFoundException("扫描运行不存在或无权访问")))
                .then(items.save(new DiscoveredItemEntity(item.id(), item.sourceId(), item.scanRunId(), item.relativeKey(),
                    item.sizeBytes(), item.modifiedAt(), item.etag(), item.mediaType(), "UNAVAILABLE",
                    item.scanGeneration(), item.createdAt(), item.version()))))
            .flatMap(saved -> events.append("source.item.unavailable", 1, "ingestion_source_item", saved.id(),
                "{\"item_id\":\"" + saved.id() + "\",\"source_id\":\"" + saved.sourceId()
                    + "\",\"reason\":\"" + (reason == null ? "unknown" : reason.replace("\"", "'")) + "\"}")
                .thenReturn(view(saved)));
    }

    private DiscoveredItemView view(DiscoveredItemEntity item) {
        return new DiscoveredItemView(item.id(), item.sourceId(), item.scanRunId(), item.relativeKey(), item.sizeBytes(),
            item.modifiedAt(), item.etag(), item.mediaType(), item.availability(), item.scanGeneration());
    }
}
