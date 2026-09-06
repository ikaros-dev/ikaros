package run.ikaros.media;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.storage.api.AttachmentReferenceQuery;

@Service
public class PersistentMediaTechnicalMetadataService implements MediaTechnicalMetadataService {
    private final MediaReleaseRepository releases;
    private final MediaProbeRepository probes;
    private final MediaExternalSubtitleRepository subtitles;
    private final AttachmentReferenceQuery attachments;
    public PersistentMediaTechnicalMetadataService(MediaReleaseRepository releases, MediaProbeRepository probes,
        MediaExternalSubtitleRepository subtitles, AttachmentReferenceQuery attachments) {
        this.releases = releases; this.probes = probes; this.subtitles = subtitles; this.attachments = attachments;
    }
    @Override public Mono<MediaProbeView> upsertProbe(UUID ownerId, UUID releaseId, UpsertMediaProbeRequest request) {
        return ownedRelease(ownerId, releaseId).flatMap(release -> {
            return probes.findByReleaseIdAndProbeProfileVersion(releaseId, request.probeProfileVersion())
                .flatMap(old -> probes.save(new MediaProbeEntity(old.id(), releaseId, request.container(), request.durationMillis(),
                    request.bitrate(), request.width(), request.height(), request.frameRate(), request.videoCodec(), request.audioCodec(),
                    request.probeProfileVersion(), request.streams(), Instant.now(), old.version())))
                .switchIfEmpty(Mono.defer(() -> probes.save(new MediaProbeEntity(null, releaseId, request.container(), request.durationMillis(),
                    request.bitrate(), request.width(), request.height(), request.frameRate(), request.videoCodec(), request.audioCodec(),
                    request.probeProfileVersion(), request.streams(), Instant.now(), null))));
        }).map(this::probeView);
    }
    @Override public Mono<MediaProbeView> getProbe(UUID ownerId, UUID releaseId, String profileVersion) {
        return ownedRelease(ownerId, releaseId).then(probes.findByReleaseIdAndProbeProfileVersion(releaseId, profileVersion).switchIfEmpty(Mono.error(new NotFoundException("Media Probe 不存在"))).map(this::probeView));
    }
    @Override public Flux<MediaExternalSubtitleView> listSubtitles(UUID ownerId, UUID releaseId) { return ownedRelease(ownerId, releaseId).flatMapMany(r -> subtitles.findAllByReleaseIdOrderByLanguageAsc(releaseId).take(100).map(this::subtitleView)); }
    @Override public Mono<MediaExternalSubtitleView> addSubtitle(UUID ownerId, UUID releaseId, AddExternalSubtitleRequest request) {
        return ownedRelease(ownerId, releaseId).flatMap(r -> attachments.requireActiveForResource(ownerId, r.playableResourceId(), request.attachmentId()).flatMap(a -> subtitles.save(new MediaExternalSubtitleEntity(null, releaseId, a.attachmentId(), request.language(), request.title(), request.format(), request.provider(), request.offsetMillis(), request.forced(), request.hearingImpaired(), null)))).map(this::subtitleView);
    }
    private Mono<MediaReleaseEntity> ownedRelease(UUID ownerId, UUID id) { return releases.findById(id).filter(r -> r.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Media Release 不存在"))); }
    private MediaProbeView probeView(MediaProbeEntity e) { return new MediaProbeView(e.id(), e.releaseId(), e.container(), e.durationMillis(), e.bitrate(), e.width(), e.height(), e.frameRate(), e.videoCodec(), e.audioCodec(), e.probeProfileVersion(), e.streams(), e.probedAt()); }
    private MediaExternalSubtitleView subtitleView(MediaExternalSubtitleEntity e) { return new MediaExternalSubtitleView(e.id(), e.releaseId(), e.attachmentId(), e.language(), e.title(), e.format(), e.provider(), e.offsetMillis(), e.forced(), e.hearingImpaired()); }
}
