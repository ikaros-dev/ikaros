package run.ikaros.media;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.resource.ResourceService;

@Service
public class PersistentMediaAvailabilityService implements MediaAvailabilityService {
    private final ResourceService resources;
    private final MediaReleaseRepository releases;

    public PersistentMediaAvailabilityService(ResourceService resources, MediaReleaseRepository releases) {
        this.resources = resources;
        this.releases = releases;
    }

    @Override
    public Mono<MediaAvailabilityView> get(UUID ownerId, UUID resourceId) {
        return resources.get(ownerId, resourceId)
            .then(releases.findAllByOwnerIdAndPlayableResourceIdOrderByCreatedAtDesc(ownerId, resourceId)
                .filter(release -> release.state() != MediaReleaseState.ARCHIVED)
                .next()
                .map(release -> switch (release.state()) {
                    case AVAILABLE -> new MediaAvailabilityView(resourceId, MediaAvailability.AVAILABLE, release.id(), "存在可播放 Release");
                    case CORRUPTED -> new MediaAvailabilityView(resourceId, MediaAvailability.CORRUPTED, release.id(), "最近 Release 校验失败");
                    case MISSING -> new MediaAvailabilityView(resourceId, MediaAvailability.MISSING, release.id(), "最近 Release 的内容不可用");
                    case ARCHIVED -> throw new IllegalStateException("ARCHIVED Release 不应进入 Availability 投影");
                }))
            .switchIfEmpty(releases.findAllByOwnerIdAndPlayableResourceIdOrderByCreatedAtDesc(ownerId, resourceId)
                .next()
                .map(release -> new MediaAvailabilityView(resourceId, MediaAvailability.RESTORE_REQUIRED, release.id(), "Release 已归档，需要恢复")))
            .defaultIfEmpty(new MediaAvailabilityView(resourceId, MediaAvailability.MISSING, null, "没有关联的 Media Release"));
    }
}
