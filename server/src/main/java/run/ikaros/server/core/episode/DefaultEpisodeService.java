package run.ikaros.server.core.episode;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

@Slf4j
@Service
public class DefaultEpisodeService implements EpisodeService {
    private final EpisodeRepository episodeRepository;
    private final AttachmentReferenceRepository attachmentReferenceRepository;
    private final AttachmentRepository attachmentRepository;

    /**
     * Construct.
     */
    public DefaultEpisodeService(EpisodeRepository episodeRepository,
                                 AttachmentReferenceRepository attachmentReferenceRepository,
                                 AttachmentRepository attachmentRepository) {
        this.episodeRepository = episodeRepository;
        this.attachmentReferenceRepository = attachmentReferenceRepository;
        this.attachmentRepository = attachmentRepository;
    }

    @Override
    public Mono<Episode> findMetaById(Long episodeId) {
        Assert.isTrue(episodeId >= 0, "'episodeId' must >= 0.");
        return episodeRepository.findById(episodeId)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    public Mono<Episode> findById(Long episodeId) {
        Assert.isTrue(episodeId >= 0, "'episodeId' must >= 0.");
        return episodeRepository.findById(episodeId)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()));
    }

    @Override
    public Flux<EpisodeResource> findResourcesById(Long episodeId) {
        Assert.isTrue(episodeId >= 0, "'episodeId' must >= 0.");
        return attachmentReferenceRepository
            .findAllByTypeAndReferenceIdOrderByTypeAscAttachmentIdAsc(
                AttachmentReferenceType.EPISODE, episodeId)
            .flatMap(attachmentReferenceEntity ->
                attachmentRepository.findById(attachmentReferenceEntity.getAttachmentId())
                    .map(attachmentEntity -> EpisodeResource.builder()
                        .episodeId(episodeId)
                        .attachmentId(attachmentEntity.getId())
                        .parentAttachmentId(attachmentEntity.getParentId())
                        .name(attachmentEntity.getName())
                        .url(attachmentEntity.getUrl())
                        .canRead(true)
                        .build())
            );
    }
}
