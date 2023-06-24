package run.ikaros.server.core.subject.service.impl;

import static org.springframework.data.relational.core.query.Criteria.where;
import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.store.entity.BaseEntity;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.subject.event.SubjectAddEvent;
import run.ikaros.server.core.subject.event.SubjectRemoveEvent;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.subject.vo.FindSubjectCondition;
import run.ikaros.server.store.entity.CollectionEntity;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.entity.EpisodeFileEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.entity.SubjectSyncEntity;
import run.ikaros.server.store.repository.CollectionRepository;
import run.ikaros.server.store.repository.EpisodeFileRepository;
import run.ikaros.server.store.repository.EpisodeRepository;
import run.ikaros.server.store.repository.FileRepository;
import run.ikaros.server.store.repository.SubjectRepository;
import run.ikaros.server.store.repository.SubjectSyncRepository;

@Slf4j
@Service
public class SubjectServiceImpl implements SubjectService, ApplicationContextAware {
    private final SubjectRepository subjectRepository;
    private final CollectionRepository collectionRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeFileRepository episodeFileRepository;
    private final SubjectSyncRepository subjectSyncRepository;
    private final FileRepository fileRepository;
    private final R2dbcEntityTemplate template;
    private ApplicationContext applicationContext;

    /**
     * Construct a {@link SubjectService} instance.
     *
     * @param subjectRepository     {@link SubjectEntity} repository
     * @param collectionRepository  {@link CollectionEntity} repository
     * @param episodeRepository     {@link EpisodeEntity} repository
     * @param episodeFileRepository {@link EpisodeFileEntity} repository
     * @param subjectSyncRepository {@link SubjectSyncEntity} repository
     * @param fileRepository        {@link FileEntity} repository
     * @param template              {@link R2dbcEntityTemplate}
     */
    public SubjectServiceImpl(SubjectRepository subjectRepository,
                              CollectionRepository collectionRepository,
                              EpisodeRepository episodeRepository,
                              EpisodeFileRepository episodeFileRepository,
                              SubjectSyncRepository subjectSyncRepository,
                              FileRepository fileRepository, R2dbcEntityTemplate template) {
        this.subjectRepository = subjectRepository;
        this.collectionRepository = collectionRepository;
        this.episodeRepository = episodeRepository;
        this.episodeFileRepository = episodeFileRepository;
        this.subjectSyncRepository = subjectSyncRepository;
        this.fileRepository = fileRepository;
        this.template = template;
    }

    @Override
    public Mono<Subject> findById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return subjectRepository.findById(id)
            .switchIfEmpty(
                Mono.error(new NotFoundException("Not found subject record by id: " + id)))
            .flatMap(subjectEntity -> copyProperties(subjectEntity, new Subject()))
            .checkpoint("FindSubjectEntityById")

            .flatMap(subject -> episodeRepository.findAllBySubjectId(subject.getId())
                .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()))
                .flatMap(episode -> episodeFileRepository.findAllByEpisodeId(episode.getId())
                    .flatMap(episodeFileEntity ->
                        fileRepository.findById(episodeFileEntity.getFileId())
                            .map(fileEntity -> EpisodeResource.builder()
                                .episodeId(episode.getId())
                                .fileId(episodeFileEntity.getFileId())
                                .name(fileEntity.getName())
                                .url(fileEntity.getUrl())
                                .build())
                    ).collectList()
                    .map(episode::setResources))
                .sort(Comparator.comparingDouble(Episode::getSequence))
                .collectList()
                .map(episodes -> subject
                    .setTotalEpisodes((long) episodes.size())
                    .setEpisodes(episodes))
                .switchIfEmpty(Mono.just(subject)))
            .checkpoint("FindEpisodeEntitiesBySubjectId")

            .flatMap(subject -> subjectSyncRepository.findAllBySubjectId(subject.getId())
                .flatMap(subjectSyncEntity -> copyProperties(subjectSyncEntity, new SubjectSync()))
                .collectList().map(subject::setSyncs))
            .checkpoint("FindSyncEntitiesBySubjectId");
    }

    @Override
    public Mono<Subject> findByBgmId(@Nonnull Long subjectId, Long bgmtvId) {
        Assert.isTrue(subjectId > 0, "'subjectId' must gt 0.");
        Assert.isTrue(bgmtvId > 0, "'bgmtvId' must gt 0.");
        return Mono.just(bgmtvId)
            .flatMap(
                platformId -> subjectSyncRepository.findBySubjectIdAndPlatformAndPlatformId(
                    subjectId, SubjectSyncPlatform.BGM_TV, String.valueOf(platformId)))
            .map(SubjectSyncEntity::getSubjectId)
            .flatMap(this::findById)
            .switchIfEmpty(
                Mono.error(new NotFoundException("Not found subject by bgmtv_id: " + bgmtvId)));
    }

    @Override
    public Mono<Subject> findBySyncPlatform(@Nonnull Long subjectId,
                                            @Nonnull SubjectSyncPlatform subjectSyncPlatform,
                                            @NotBlank String platformId) {
        Assert.notNull(subjectSyncPlatform, "'subjectSyncPlatform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        return subjectSyncRepository.findBySubjectIdAndPlatformAndPlatformId(subjectId,
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
    public Mono<Subject> create(Subject subject) {
        Assert.notNull(subject, "'subject' must not be null.");
        final AtomicReference<Long> subjectId = new AtomicReference<>(-1L);
        return Mono.just(subject)
            .filter(sub -> Objects.nonNull(sub.getType()))
            .switchIfEmpty(
                Mono.error(new IllegalArgumentException("subject type must not be null")))
            .checkpoint("AssertParams")

            .flatMap(sub -> copyProperties(sub, new SubjectEntity()))
            .flatMap(subjectRepository::save)
            .doOnNext(entity -> applicationContext.publishEvent(new SubjectAddEvent(this, entity)))
            .map(subjectEntity -> {
                subjectId.set(subjectEntity.getId());
                return subjectEntity;
            })
            .flatMap(subjectEntity -> copyProperties(subjectEntity, subject))
            .checkpoint("CreateSubjectEntity")

            .map(sub -> Objects.isNull(subject.getEpisodes())
                ? new ArrayList<Episode>() : subject.getEpisodes())
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.just(new ArrayList<>()))
            .flatMapMany(episodes -> Flux.fromStream(episodes.stream()))
            .flatMap(episode -> copyProperties(episode, new EpisodeEntity()))
            .map(entity -> entity.setSubjectId(subjectId.get()))
            .flatMap(episodeRepository::save)
            .flatMap(episodeEntity -> copyProperties(episodeEntity, new Episode()))
            .collectList()
            .map(subject::setEpisodes)
            .checkpoint("CreateEpisodeEntities")

            .map(sub -> Objects.isNull(subject.getSyncs())
                ? new ArrayList<SubjectSync>() : subject.getSyncs())
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.just(new ArrayList<>()))
            .flatMapMany(subjectSyncs -> Flux.fromStream(subjectSyncs.stream()))
            .flatMap(subjectSync -> copyProperties(subjectSync, new SubjectSyncEntity()))
            .map(entity -> entity.setSubjectId(subjectId.get()))
            .flatMap(subjectSyncRepository::save)
            .flatMap(subjectSyncEntity -> copyProperties(subjectSyncEntity, new SubjectSync()))
            .collectList()
            .map(subject::setSyncs)
            .checkpoint("CreateSubjectSyncEntities");
    }

    @Override
    public Mono<Void> update(Subject subject) {
        Assert.notNull(subject, "'subject' must not null.");
        Long id = subject.getId();
        Assert.isTrue(id > 0, "'subject id' must gt 0.");
        AtomicReference<Long> subjectId = new AtomicReference<>(id);
        return Mono.just(subject)
            .flatMap(sub -> copyProperties(sub, new SubjectEntity()))
            .flatMap(entity -> {
                Map<SqlIdentifier, Object> map = new HashMap<>();
                map.put(SqlIdentifier.unquoted("update_time"), entity.getUpdateTime());
                map.put(SqlIdentifier.unquoted("name"), entity.getName());
                map.put(SqlIdentifier.unquoted("nameCn"), entity.getNameCn());
                map.put(SqlIdentifier.unquoted("cover"), entity.getCover());
                map.put(SqlIdentifier.unquoted("type"), entity.getType());
                map.put(SqlIdentifier.unquoted("infobox"), entity.getInfobox());
                map.put(SqlIdentifier.unquoted("summary"), entity.getSummary());
                map.put(SqlIdentifier.unquoted("nsfw"), entity.getNsfw());
                map.put(SqlIdentifier.unquoted("air_time"), entity.getAirTime());
                return Mono.just(map)
                    .flatMap(columnsToUpdate -> template
                        .update(Query.query(where("id").is(subjectId.get())),
                            Update.from(columnsToUpdate),
                            SubjectEntity.class))
                    .filter(count -> count > 0)
                    .switchIfEmpty(Mono.error(new RuntimeException(
                        "Update subject entity fail for id: " + entity.getId())));
            })
            .doOnSuccess(count
                -> log.debug("Update subject entity success for id=[{}]", subjectId.get()))
            .checkpoint("UpdateSubjectEntity")

            // 剧集的更新逻辑是: 移除原有的所有剧集记录,再创建新的剧集记录.
            .then(episodeRepository.deleteAllBySubjectId(subjectId.get()))
            .then(Mono.justOrEmpty(subject.getEpisodes()))
            .flatMapMany(episodes -> Flux.fromStream(episodes.stream()))
            .flatMap(episode -> copyProperties(episode, new EpisodeEntity()))
            .map(entity -> entity.setSubjectId(subjectId.get()))
            .flatMap(episodeRepository::save)
            .checkpoint("UpdateEpisodeEntities")

            // 条目同步的更新逻辑是: 移除原有的所有记录,再创建新的记录.
            .then(subjectSyncRepository.deleteAllBySubjectId(subjectId.get()))
            .then(Mono.justOrEmpty(subject.getSyncs()))
            .flatMapMany(subjectSyncs -> Flux.fromStream(subjectSyncs.stream()))
            .flatMap(subjectSync -> copyProperties(subjectSync, new SubjectSyncEntity()))
            .map(entity -> entity.setSubjectId(subjectId.get()))
            .flatMap(subjectSyncRepository::save)
            .checkpoint("UpdateSubjectSyncEntities")
            .then();
    }

    @Override
    public Mono<Void> deleteById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return subjectRepository.existsById(id)
            .filter(flag -> flag)
            .flatMap(flag -> subjectRepository.findById(id))
            .doOnNext(entity -> applicationContext.publishEvent(
                new SubjectRemoveEvent(this, entity)))
            // Delete subject entity
            .flatMap(entity -> subjectRepository.deleteById(id))
            // Delete episode entities
            .then(episodeRepository.deleteAllBySubjectId(id))
            // Delete subject sync entities
            .then(subjectSyncRepository.deleteAllBySubjectId(id))
            .then()
            ;
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
        // todo 使用 R2dbcEntityTemplate 进行动态拼接
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

    @Override
    public Mono<Void> deleteAll() {
        return subjectRepository.findAll()
            .map(BaseEntity::getId)
            .flatMap(this::deleteById)
            .checkpoint("DeleteAllSubject")
            .then();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
