package run.ikaros.server.core.subject.service.impl;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.exception.RegexMatchingException;
import run.ikaros.api.infra.utils.RegexUtils;
import run.ikaros.api.store.enums.FileType;
import run.ikaros.server.core.subject.event.EpisodeFileUpdateEvent;
import run.ikaros.server.core.subject.service.EpisodeFileService;
import run.ikaros.server.store.entity.EpisodeFileEntity;
import run.ikaros.server.store.entity.FileEntity;
import run.ikaros.server.store.repository.EpisodeFileRepository;
import run.ikaros.server.store.repository.EpisodeRepository;
import run.ikaros.server.store.repository.FileRepository;

@Slf4j
@Service
public class EpisodeServiceImpl implements EpisodeFileService {
    private final EpisodeFileRepository episodeFileRepository;
    private final EpisodeRepository episodeRepository;
    private final FileRepository fileRepository;
    private final ApplicationEventPublisher applicationEventPublisher;

    /**
     * Construct.
     */
    public EpisodeServiceImpl(EpisodeFileRepository episodeFileRepository,
                              EpisodeRepository episodeRepository, FileRepository fileRepository,
                              ApplicationEventPublisher applicationEventPublisher) {
        this.episodeFileRepository = episodeFileRepository;
        this.episodeRepository = episodeRepository;
        this.fileRepository = fileRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    public Mono<Void> create(@Nonnull Long episodeId, @Nonnull Long fileId) {
        Assert.isTrue(episodeId > 0, "'episodeId' must gt 0.");
        Assert.isTrue(fileId > 0, "'episodeId' must gt 0.");

        return episodeFileRepository.existsByEpisodeIdAndFileId(episodeId, fileId)
            .filter(exists -> !exists)
            .flatMap(exists -> episodeFileRepository.save(EpisodeFileEntity.builder()
                .episodeId(episodeId).fileId(fileId)
                .build()))
            .then();
    }

    @Override
    public Mono<Void> remove(@NotNull Long episodeId, @NotNull Long fileId) {
        Assert.isTrue(episodeId > 0, "'episodeId' must gt 0.");
        Assert.isTrue(fileId > 0, "'episodeId' must gt 0.");
        return episodeFileRepository.existsByEpisodeIdAndFileId(episodeId, fileId)
            .filter(exists -> exists)
            .flatMap(exists -> episodeFileRepository.deleteByEpisodeIdAndFileId(episodeId, fileId))
            .then();
    }

    @Override
    public Mono<Void> batchMatching(@NotNull Long subjectId, @NotNull Long[] fileIds) {
        return batchMatching(subjectId, fileIds, false);
    }

    @Override
    public Mono<Void> batchMatching(@NotNull Long subjectId, @NotNull Long[] fileIds,
                                    boolean notify) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt 0.");
        Assert.notNull(fileIds, "'fileIds' must not null.");
        return Flux.fromArray(fileIds)
            .flatMap(fileRepository::findById)
            .filter(entity -> FileType.VIDEO.equals(entity.getType()))
            .flatMap(fileEntity -> getSeqMono(fileEntity)
                .flatMap(seq -> episodeRepository.findBySubjectIdAndSequence(subjectId,
                    Double.valueOf(String.valueOf(seq))))
                .flatMap(episodeEntity -> episodeFileRepository
                    .existsByEpisodeId(episodeEntity.getId())
                    .filter(exists -> !exists)
                    .flatMap(exists -> episodeFileRepository
                        .save(EpisodeFileEntity.builder()
                            .fileId(fileEntity.getId())
                            .episodeId(episodeEntity.getId())
                            .build())
                        .doOnSuccess(episodeFileEntity -> {
                            log.info("save episode file matching "
                                    + "for file name:[{}] and episode seq:[{}] "
                                    + "when subjectId=[{}].",
                                fileEntity.getName(), episodeEntity.getSequence(), subjectId);
                            EpisodeFileUpdateEvent event =
                                new EpisodeFileUpdateEvent(this,
                                    episodeEntity.getId(),
                                    fileEntity.getId(), notify);
                            applicationEventPublisher.publishEvent(event);
                            log.debug("publish event EpisodeFileUpdateEvent "
                                + "for episodeFileEntity: {}", episodeFileEntity);
                        }))
                ))
            .then();

    }

    private static Mono<Double> getSeqMono(FileEntity entity) {
        Double seq;
        try {
            seq = Double.valueOf(RegexUtils.parseEpisodeSeqByFileName(entity.getName()));
        } catch (RegexMatchingException regexMatchingException) {
            log.warn("parse episode seq by file name fail", regexMatchingException);
            return Mono.empty();
        }
        return Mono.justOrEmpty(seq);
    }
}
