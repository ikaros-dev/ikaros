package run.ikaros.media;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.ResourceService;

@Service
public class PersistentPlaybackSourceService implements PlaybackSourceService {
    private final ResourceService resources;
    private final MediaReleaseRepository releases;
    public PersistentPlaybackSourceService(ResourceService resources, MediaReleaseRepository releases) { this.resources = resources; this.releases = releases; }
    @Override public Mono<PlaybackSourceView> resolve(UUID ownerId, UUID resourceId, UUID preferredReleaseId) {
        return resources.get(ownerId, resourceId).then((preferredReleaseId == null
            ? releases.findAllByOwnerIdAndPlayableResourceIdOrderByCreatedAtDesc(ownerId, resourceId).filter(r -> r.state() == MediaReleaseState.AVAILABLE).next()
            : releases.findById(preferredReleaseId).filter(r -> r.ownerId().equals(ownerId) && r.playableResourceId().equals(resourceId) && r.state() == MediaReleaseState.AVAILABLE)))
            .switchIfEmpty(Mono.error(new NotFoundException("没有可播放的 Media Release")))
            .map(r -> new PlaybackSourceView(r.id(), r.attachmentId(), PlaybackSourceMode.DIRECT_PLAY,
                preferredReleaseId == null ? "选择最近创建的可用 Release" : "使用请求指定的可用 Release",
                "/api/attachments/" + r.attachmentId() + "/content"));
    }
}
