package run.ikaros.server.core.subject;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.store.entity.BaseEntity;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.CollectionEntity;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.entity.EpisodeFileEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.entity.SubjectImageEntity;
import run.ikaros.server.store.entity.SubjectSyncEntity;
import run.ikaros.server.store.repository.CollectionRepository;
import run.ikaros.server.store.repository.EpisodeFileRepository;
import run.ikaros.server.store.repository.EpisodeRepository;
import run.ikaros.server.store.repository.SubjectImageRepository;
import run.ikaros.server.store.repository.SubjectRepository;
import run.ikaros.server.store.repository.SubjectSyncRepository;

@Service
public class SubjectServiceImpl implements SubjectService {
    private final SubjectRepository subjectRepository;
    private final CollectionRepository collectionRepository;
    private final SubjectImageRepository subjectImageRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeFileRepository episodeFileRepository;
    private final SubjectSyncRepository subjectSyncRepository;

    /**
     * Construct a {@link SubjectService} instance.
     *
     * @param subjectRepository      {@link SubjectEntity} repository
     * @param collectionRepository   {@link CollectionEntity} repository
     * @param subjectImageRepository {@link SubjectImageEntity} repository
     * @param episodeRepository      {@link EpisodeEntity} repository
     * @param episodeFileRepository  {@link EpisodeFileEntity} repository
     * @param subjectSyncRepository  {@link SubjectSyncEntity} repository
     */
    public SubjectServiceImpl(SubjectRepository subjectRepository,
                              CollectionRepository collectionRepository,
                              SubjectImageRepository subjectImageRepository,
                              EpisodeRepository episodeRepository,
                              EpisodeFileRepository episodeFileRepository,
                              SubjectSyncRepository subjectSyncRepository) {
        this.subjectRepository = subjectRepository;
        this.collectionRepository = collectionRepository;
        this.subjectImageRepository = subjectImageRepository;
        this.episodeRepository = episodeRepository;
        this.episodeFileRepository = episodeFileRepository;
        this.subjectSyncRepository = subjectSyncRepository;
    }

    @Override
    public Mono<Subject> findById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return subjectRepository.findById(id)
            .switchIfEmpty(
                Mono.error(new NotFoundException("Not found subject record by id: " + id)))
            .flatMap(subjectEntity -> copyProperties(subjectEntity, new Subject())
                .map(subject -> subject.setType(subjectEntity.getType())))
            .flatMap(subject -> subjectImageRepository.findBySubjectId(subject.getId())
                .flatMap(
                    subjectImageEntity -> copyProperties(subjectImageEntity, new SubjectImage()))
                .flatMap(subjectImage -> Mono.just(subject.setImage(subjectImage)))
                .switchIfEmpty(Mono.just(subject)))
            .flatMap(subject -> episodeRepository.findBySubjectId(subject.getId())
                .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()))
                .collectList()
                .flatMap(episodes -> Mono.just(subject.setTotalEpisodes((long) episodes.size())
                    .setEpisodes(episodes)))
                .switchIfEmpty(Mono.just(subject)))
            .flatMap(subject -> subjectSyncRepository.findAllBySubjectId(subject.getId())
                .flatMap(subjectSyncEntity -> copyProperties(subjectSyncEntity, new SubjectSync()))
                .collectList().map(subject::setSyncs));
    }

    @Override
    public Mono<Subject> findByBgmId(Long bgmtvId) {
        Assert.isTrue(bgmtvId > 0, "'bgmtvId' must gt 0.");
        return Mono.just(bgmtvId)
            .flatMap(platformId -> subjectSyncRepository.findByPlatformAndPlatformId(
                SubjectSyncPlatform.BGM_TV, platformId.toString()))
            .map(SubjectSyncEntity::getSubjectId)
            .flatMap(this::findById)
            .switchIfEmpty(
                Mono.error(new NotFoundException("Not found subject by bgmtv_id: " + bgmtvId)))
            .flatMap(subjectEntity -> findById(subjectEntity.getId()));
    }

    @Override
    public Mono<Subject> findBySyncPlatform(@Nonnull SubjectSyncPlatform subjectSyncPlatform,
                                            @NotBlank String platformId) {
        Assert.notNull(subjectSyncPlatform, "'subjectSyncPlatform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        return subjectSyncRepository.findByPlatformAndPlatformId(
                subjectSyncPlatform, platformId)
            .map(SubjectSyncEntity::getSubjectId)
            .flatMap(this::findById)
            .switchIfEmpty(
                Mono.error(new NotFoundException(
                    "Not found subject by sync platform and platformId: "
                        + subjectSyncPlatform + "-" + platformId)))
            .flatMap(subjectEntity -> findById(subjectEntity.getId()));
    }

    @Override
    public Mono<Subject> save(Subject subject) {
        Assert.notNull(subject, "'subject' must not be null.");
        return Mono.just(subject)
            .filter(subject1 -> Objects.nonNull(subject1.getType()))
            .switchIfEmpty(
                Mono.error(new IllegalArgumentException("subject type must not be null")))
            // Save subject entity
            .flatMap(subject1 -> copyProperties(subject1, new SubjectEntity())
                .map(subjectEntity -> subjectEntity
                    .setType(subject1.getType()))
                .flatMap(subjectRepository::save)
                .flatMap(subjectEntity -> Mono.just(subject1.setId(subjectEntity.getId()))
                    .map(subject2 ->
                        subject2.setType(subjectEntity.getType()))))
            // Save subject image entity
            .flatMap(subject1 -> Mono.just(subject1.getImage())
                .flatMap(subjectImage -> copyProperties(subjectImage, new SubjectImageEntity())
                    .map(subjectImageEntity -> subjectImageEntity.setSubjectId(subject1.getId()))
                    .flatMap(subjectImageRepository::save)
                    .map(subjectImageEntity -> subjectImage.setId(subjectImageEntity.getId()))
                    .flatMap(subjectImage1 -> Mono.just(subject1.setImage(subjectImage1)))))
            // Save episode entity
            .flatMap(subject1 -> Mono.just(subject1.getEpisodes())
                .flatMapMany(episodes -> Flux.fromStream(episodes.stream()))
                .flatMap(episode -> copyProperties(episode, new EpisodeEntity())
                    .map(episodeEntity -> episodeEntity.setSubjectId(subject1.getId()))
                    .flatMap(episodeRepository::save)
                    .flatMap(episodeEntity -> Mono.just(episode.setId(episodeEntity.getId()))))
                .collectList()
                .flatMap(episodes -> Mono.just(subject1.setEpisodes(episodes))));
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return subjectRepository.existsById(id)
            .filter(flag -> flag)
            // Delete subject entity
            .flatMap(subject -> subjectRepository.deleteById(id))
            // Delete subject image entity
            .then(subjectImageRepository.deleteBySubjectId(id))
            // Delete episode entities
            .then(episodeRepository.deleteAllBySubjectId(id));
    }

    @Override
    public Mono<PagingWrap<Subject>> findAllByPageable(PagingWrap<Subject> pagingWrap) {
        Assert.notNull(pagingWrap, "'pagingWrap' must not be null");
        Assert.isTrue(pagingWrap.getPage() > 0, "'pagingWrap' page must gt 0");
        Assert.isTrue(pagingWrap.getSize() > 0, "'pagingWrap' size must gt 0");
        return Mono.just(pagingWrap)
            .flatMap(pagingWrap1 ->
                subjectRepository.findAllBy(
                        PageRequest.of(pagingWrap1.getPage() - 1, pagingWrap1.getSize()))
                    .map(BaseEntity::getId)
                    .flatMap(this::findById)
                    .collectList()
                    .flatMap(subjects -> subjectRepository.count()
                        .flatMap(total -> Mono.just(
                            new PagingWrap<>(pagingWrap1.getPage(), pagingWrap1.getSize(), total,
                                subjects)))));
    }

    @Override
    public Mono<PagingWrap<Subject>> listEntitiesByCondition(FindSubjectCondition condition) {
        Assert.notNull(condition, "'condition' must not null.");
        Integer page = condition.getPage();
        Integer size = condition.getSize();
        String name = condition.getName();
        String nameLike = "%" + name + "%";
        String nameCn = condition.getNameCn();
        String nameCnLike = "%" + nameCn + "%";
        Boolean nsfw = condition.getNsfw();
        SubjectType type = condition.getType();

        PageRequest pageRequest = PageRequest.of(page - 1, size);

        Flux<SubjectEntity> subjectEntityFlux;
        Mono<Long> countMono;
        if (name == null) {
            if (nameCn == null) {
                if (type == null) {
                    if (nsfw == null) {
                        subjectEntityFlux = subjectRepository.findAllBy(pageRequest);
                        countMono = subjectRepository.count();
                    } else {
                        subjectEntityFlux = subjectRepository.findAllByNsfw(nsfw, pageRequest);
                        countMono = subjectRepository.countAllByNsfw(nsfw);
                    }
                } else {
                    if (nsfw == null) {
                        subjectEntityFlux =
                            subjectRepository.findAllByType(type, pageRequest);
                        countMono = subjectRepository.countAllByType(type);
                    } else {
                        subjectEntityFlux =
                            subjectRepository.findAllByNsfwAndType(nsfw, type,
                                pageRequest);
                        countMono = subjectRepository.countAllByNsfwAndType(nsfw, type);
                    }
                }
            } else {
                if (type == null) {
                    if (nsfw == null) {
                        subjectEntityFlux =
                            subjectRepository.findAllByNameCnLike(nameCnLike, pageRequest);
                        countMono = subjectRepository.countAllByNameCnLike(nameCnLike);
                    } else {
                        subjectEntityFlux =
                            subjectRepository.findAllByNsfwAndNameCnLike(nsfw, nameCnLike,
                                pageRequest);
                        countMono = subjectRepository.countAllByNsfwAndNameCnLike(nsfw, nameCnLike);
                    }
                } else {
                    if (nsfw == null) {
                        subjectEntityFlux =
                            subjectRepository.findAllByNameCnLikeAndType(nameCnLike, type,
                                pageRequest);
                        countMono = subjectRepository.countAllByNameCnLikeAndType(nameCnLike,
                            type);
                    } else {
                        subjectEntityFlux =
                            subjectRepository.findAllByNsfwAndNameCnLikeAndType(nsfw, nameCnLike,
                                type, pageRequest);
                        countMono =
                            subjectRepository.countAllByNsfwAndNameCnLikeAndType(nsfw, nameCnLike,
                                type);
                    }
                }
            }
        } else {
            if (nameCn == null) {
                if (type == null) {
                    if (nsfw == null) {
                        subjectEntityFlux =
                            subjectRepository.findAllByNameLike(nameLike, pageRequest);
                        countMono =
                            subjectRepository.countAllByNameLike(nameLike);
                    } else {
                        subjectEntityFlux =
                            subjectRepository.findAllByNsfwAndNameLike(nsfw, nameLike, pageRequest);
                        countMono =
                            subjectRepository.countAllByNsfwAndNameLike(nsfw, nameLike);
                    }
                } else {
                    if (nsfw == null) {
                        subjectEntityFlux =
                            subjectRepository.findAllByNameLikeAndType(nameLike, type,
                                pageRequest);
                        countMono =
                            subjectRepository.countAllByNameLikeAndType(nameLike, type);
                    } else {
                        subjectEntityFlux =
                            subjectRepository.findAllByNsfwAndNameLikeAndType(nsfw, nameLike,
                                type, pageRequest);
                        countMono =
                            subjectRepository.countAllByNsfwAndNameLikeAndType(nsfw, nameLike,
                                type);
                    }
                }
            } else {
                if (type == null) {
                    if (nsfw == null) {
                        subjectEntityFlux =
                            subjectRepository.findAllByNameLikeAndNameCnLike(nameLike, nameCnLike,
                                pageRequest);
                        countMono =
                            subjectRepository.countAllByNameLikeAndNameCnLike(nameLike, nameCnLike);
                    } else {
                        subjectEntityFlux =
                            subjectRepository.findAllByNsfwAndNameLikeAndNameCnLike(nsfw, nameLike,
                                nameCnLike, pageRequest);
                        countMono =
                            subjectRepository.countAllByNsfwAndNameLikeAndNameCnLike(nsfw, nameLike,
                                nameCnLike);
                    }
                } else {
                    if (nsfw == null) {
                        subjectEntityFlux =
                            subjectRepository.findAllByNameLikeAndNameCnLikeAndType(nameLike,
                                nameCnLike, type, pageRequest);
                        countMono =
                            subjectRepository.countAllByNameLikeAndNameCnLikeAndType(nameLike,
                                nameCnLike, type);
                    } else {
                        subjectEntityFlux =
                            subjectRepository.findAllByNsfwAndNameLikeAndNameCnLikeAndType(nsfw,
                                nameLike, nameCnLike, type, pageRequest);
                        countMono =
                            subjectRepository.countAllByNsfwAndNameLikeAndNameCnLikeAndType(nsfw,
                                nameLike, nameCnLike, type);
                    }
                }
            }
        }

        return subjectEntityFlux.map(BaseEntity::getId)
            .flatMap(this::findById)
            .collectList()
            .flatMap(subjects -> countMono
                .map(count -> new PagingWrap<>(page, size, count, subjects)));
    }
}
