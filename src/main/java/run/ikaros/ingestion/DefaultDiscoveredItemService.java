package run.ikaros.ingestion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class DefaultDiscoveredItemService implements DiscoveredItemService {
    private final ScanRunRepository scanRuns;
    private final DiscoveredItemRepository items;

    public DefaultDiscoveredItemService(ScanRunRepository scanRuns, DiscoveredItemRepository items) {
        this.scanRuns = scanRuns;
        this.items = items;
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
            .thenMany(items.findAllByScanRunIdOrderByRelativeKeyAsc(scanRunId))
            .map(this::view).collectList();
    }

    private DiscoveredItemView view(DiscoveredItemEntity item) {
        return new DiscoveredItemView(item.id(), item.sourceId(), item.scanRunId(), item.relativeKey(), item.sizeBytes(),
            item.modifiedAt(), item.etag(), item.mediaType(), item.availability(), item.scanGeneration());
    }
}
