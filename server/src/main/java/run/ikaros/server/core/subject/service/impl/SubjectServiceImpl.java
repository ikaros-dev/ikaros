package run.ikaros.server.core.subject.service.impl;

import static org.springframework.data.relational.core.query.Criteria.where;
import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.SqlIdentifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.vo.FindSubjectCondition;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.cache.annotation.MonoCacheEvict;
import run.ikaros.server.core.subject.event.SubjectAddEvent;
import run.ikaros.server.core.subject.event.SubjectRemoveEvent;
import run.ikaros.server.core.subject.event.SubjectUpdateEvent;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.entity.BaseEntity;
import run.ikaros.server.store.entity.EpisodeEntity;
import run.ikaros.server.store.entity.SubjectCollectionEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.entity.SubjectSyncEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.EpisodeRepository;
import run.ikaros.server.store.repository.SubjectCollectionRepository;
import run.ikaros.server.store.repository.SubjectRepository;
import run.ikaros.server.store.repository.SubjectSyncRepository;

@Slf4j
@Service
public class SubjectServiceImpl implements SubjectService, ApplicationContextAware {
    private final SubjectRepository subjectRepository;
    private final SubjectCollectionRepository subjectCollectionRepository;
    private final EpisodeRepository episodeRepository;
    private final AttachmentRepository attachmentRepository;
    private final AttachmentReferenceRepository attachmentReferenceRepository;
    private final SubjectSyncRepository subjectSyncRepository;
    private final R2dbcEntityTemplate template;
    private ApplicationContext applicationContext;

    /**
     * Construct a {@link SubjectService} instance.
     *
     * @param subjectRepository             {@link SubjectEntity} repository
     * @param subjectCollectionRepository   {@link SubjectCollectionEntity} repository
     * @param episodeRepository             {@link EpisodeEntity} repository
     * @param attachmentRepository          {@link AttachmentEntity} repository
     * @param attachmentReferenceRepository {@link AttachmentReferenceEntity} repository
     * @param subjectSyncRepository         {@link SubjectSyncEntity} repository
     * @param template                      {@link R2dbcEntityTemplate}
     */
    public SubjectServiceImpl(SubjectRepository subjectRepository,
                              SubjectCollectionRepository subjectCollectionRepository,
                              EpisodeRepository episodeRepository,
                              AttachmentRepository attachmentRepository,
                              AttachmentReferenceRepository attachmentReferenceRepository,
                              SubjectSyncRepository subjectSyncRepository,
                              R2dbcEntityTemplate template) {
        this.subjectRepository = subjectRepository;
        this.subjectCollectionRepository = subjectCollectionRepository;
        this.episodeRepository = episodeRepository;
        this.attachmentRepository = attachmentRepository;
        this.attachmentReferenceRepository = attachmentReferenceRepository;
        this.subjectSyncRepository = subjectSyncRepository;
        this.template = template;
    }

    @Override
    public Mono<Subject> findById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return subjectRepository.findById(id)
            .flatMap(subjectEntity -> copyProperties(subjectEntity, new Subject()));
    }

    private boolean getSubjectCanReadByEpisodes(List<Episode> episodes) {
        if (episodes == null || episodes.isEmpty()) {
            return false;
        }

        return true;
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
    public Mono<Subject> findBySubjectIdAndPlatformAndPlatformId(@Nonnull Long subjectId,
                                                                 @Nonnull SubjectSyncPlatform
                                                                     platform,
                                                                 @NotBlank String platformId) {
        Assert.notNull(platform, "'platform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        return subjectSyncRepository.findBySubjectIdAndPlatformAndPlatformId(subjectId,
                platform, platformId)
            .map(SubjectSyncEntity::getSubjectId)
            .flatMap(this::findById).flatMap(subjectEntity -> findById(subjectEntity.getId()));
    }

    @Override
    public Flux<Subject> findByPlatformAndPlatformId(
        @Nonnull SubjectSyncPlatform subjectSyncPlatform, String platformId) {
        Assert.notNull(subjectSyncPlatform, "'subjectSyncPlatform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        return subjectSyncRepository.findByPlatformAndPlatformId(subjectSyncPlatform, platformId)
            .map(SubjectSyncEntity::getSubjectId).flatMap(this::findById)
            .flatMap(subjectEntity -> findById(subjectEntity.getId()));
    }

    @Override
    public Mono<Boolean> existsByPlatformAndPlatformId(
        @Nonnull SubjectSyncPlatform subjectSyncPlatform, String platformId) {
        Assert.notNull(subjectSyncPlatform, "'subjectSyncPlatform' must not null.");
        Assert.hasText(platformId, "'platformId' must has text.");
        return subjectSyncRepository.existsByPlatformAndPlatformId(subjectSyncPlatform, platformId);
    }

    @Override
    public synchronized Mono<Subject> create(Subject subject) {
        Assert.notNull(subject, "'subject' must not be null.");
        Assert.notNull(subject.getType(), "'subject.type' must not be null.");
        return copyProperties(subject, new SubjectEntity())
            .flatMap(subjectRepository::save)
            .doOnNext(entity -> applicationContext.publishEvent(new SubjectAddEvent(this, entity)))
            .flatMap(subjectEntity -> copyProperties(subjectEntity, subject));
    }

    private Mono<SubjectEntity> publishSubjectUpdateEvent(SubjectEntity subjectEntity) {
        Long subjectId = subjectEntity.getId();
        return subjectRepository.findById(subjectId)
            .doOnSuccess(oldEntity -> {
                SubjectUpdateEvent event = new SubjectUpdateEvent(this, oldEntity, subjectEntity);
                applicationContext.publishEvent(event);
                log.debug("publish SubjectUpdateEvent: [{}]", event);
            })
            .then(Mono.just(subjectEntity));
    }

    @Override
    public Mono<Void> update(Subject subject) {
        Assert.notNull(subject, "'subject' must not null.");
        Assert.isTrue(subject.getId() > 0, "'subject id' must gt 0.");
        return copyProperties(subject, new SubjectEntity())
            .flatMap(this::publishSubjectUpdateEvent)
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
                        .update(Query.query(where("id").is(entity.getId())),
                            Update.from(columnsToUpdate),
                            SubjectEntity.class))
                    .filter(count -> count > 0)
                    .switchIfEmpty(Mono.error(new RuntimeException(
                        "Update subject entity fail for id: " + entity.getId())));
            })
            .doOnSuccess(count
                -> log.debug("Update subject entity success for count=[{}]", count))
            .checkpoint("UpdateSubjectEntity")

            .then();
    }

    /**
     * 不存在则新增，存在则忽略.
     */
    private Mono<SubjectSyncEntity> updateSubjectSyncEntity(SubjectSyncEntity entity) {
        Assert.notNull(entity, "'subjectSyncEntity' must not null.");
        return subjectSyncRepository.existsBySubjectIdAndPlatformAndPlatformId(
                entity.getSubjectId(), entity.getPlatform(), entity.getPlatformId())
            .filter(exists -> exists)
            .flatMap(exists -> subjectSyncRepository.findBySubjectIdAndPlatformAndPlatformId(
                entity.getSubjectId(), entity.getPlatform(), entity.getPlatformId()))
            .switchIfEmpty(Mono.just(entity)
                .doOnSuccess(e2 -> log.debug("create new subject sync record: [{}].", e2))
                .flatMap(subjectSyncRepository::save));
    }

    @Override
    @MonoCacheEvict
    public Mono<Void> deleteById(Long id) {
        Assert.isTrue(id > 0, "'id' must gt 0.");
        return subjectRepository.existsById(id)
            .filter(flag -> flag)
            .flatMap(flag -> subjectRepository.findById(id))
            .doOnNext(entity -> applicationContext.publishEvent(
                new SubjectRemoveEvent(this, entity)))
            // Delete subject entity
            .flatMap(entity -> subjectRepository.deleteById(id))
            // Delete all episode entities and episode refs
            .thenMany(episodeRepository.findAllBySubjectId(id))
            .flatMap(episodeEntity ->
                attachmentReferenceRepository.deleteAllByTypeAndReferenceId(
                        AttachmentReferenceType.EPISODE, episodeEntity.getId())
                    .then(episodeRepository.delete(episodeEntity)))
            // Delete subject sync entities
            .then(subjectSyncRepository.deleteAllBySubjectId(id))
            .then()
            ;
    }

    private Mono<Long> findMatchingEpisodeCount(Long subjectId) {
        return episodeRepository.findAllBySubjectId(subjectId)
            .map(EpisodeEntity::getId)
            .filterWhen(epId -> attachmentReferenceRepository.existsByTypeAndReferenceId(
                AttachmentReferenceType.EPISODE, epId
            ))
            .collectList()
            .map(List::size)
            .map(Long::valueOf);
    }

    @Override
    public Mono<PagingWrap<Subject>> findAllByPageable(PagingWrap<Subject> pw) {
        Assert.notNull(pw, "'pagingWrap' must not be null");
        Assert.isTrue(pw.getPage() > 0, "'pagingWrap' page must gt 0");
        Assert.isTrue(pw.getSize() > 0, "'pagingWrap' size must gt 0");
        return subjectRepository.findAllByOrderByAirTimeDesc(
                PageRequest.of(pw.getPage() - 1, pw.getSize()))
            .flatMap(entity -> copyProperties(entity, new Subject()))
            .collectList()
            .flatMap(subjects -> subjectRepository.count()
                .flatMap(total -> Mono.just(
                    new PagingWrap<>(pw.getPage(), pw.getSize(), total,
                        subjects))));
    }

    @Override
    public Mono<PagingWrap<Subject>> listEntitiesByCondition(FindSubjectCondition condition) {
        Assert.notNull(condition, "'condition' must not null.");
        condition.initDefaultIfNull();
        Integer page = condition.getPage();
        Integer size = condition.getSize();
        String name = condition.getName();
        String nameLike = "%" + name + "%";
        String nameCn = condition.getNameCn();
        String nameCnLike = "%" + nameCn + "%";
        Boolean nsfw = condition.getNsfw();
        final SubjectType type = condition.getType();
        final String time = condition.getTime();
        final Boolean airTimeDesc = condition.getAirTimeDesc();
        final Boolean updateTimeDesc = condition.getUpdateTimeDesc();

        final PageRequest pageRequest = PageRequest.of(page - 1, size);

        Criteria criteria = Criteria.empty();

        if (Objects.nonNull(nsfw)) {
            criteria =
                Criteria.where("nsfw").is(nsfw);
        }

        if (StringUtils.isNotBlank(name)) {
            criteria = criteria.and("name").like(nameLike);
        }
        if (StringUtils.isNotBlank(nameCn)) {
            criteria = criteria.and("name_cn").like(nameCnLike);
        }
        if (!Objects.isNull(type)) {
            criteria = criteria.and("type").is(type);
        }

        if (StringUtils.isNotBlank(time)) {
            if (time.indexOf('-') > 0) {
                // 日期范围，例如；2000.9-2010.8
                String[] split = time.split("-");
                String first = split[0];
                String second = split[1];
                LocalDateTime startTime;
                if (first.indexOf(".") > 0) {
                    String[] split1 = first.split("\\.");
                    startTime =
                        Year.parse(split1[0]).atMonth(Integer.parseInt(split1[1])).atDay(1)
                            .atStartOfDay();
                } else {
                    startTime = Year.parse(first).atMonth(1).atDay(1).atStartOfDay();
                }
                LocalDateTime endTime;
                if (second.indexOf(".") > 0) {
                    String[] split2 = second.split("\\.");
                    endTime =
                        Year.parse(split2[0]).atMonth(Integer.parseInt(split2[1])).atDay(1)
                            .atStartOfDay().plusMonths(1);
                } else {
                    endTime = Year.parse(second).atDay(1).atStartOfDay().plusMonths(1);
                }
                criteria = criteria.and(Criteria.where("air_time").between(startTime, endTime));
            } else {
                // 单个类型，例如：2020.8
                if (time.indexOf('.') > 0) {
                    String[] split = time.split("\\.");
                    LocalDateTime startTime =
                        Year.parse(split[0]).atMonth(Integer.parseInt(split[1])).atDay(1)
                            .atStartOfDay();
                    criteria = criteria.and(
                        Criteria.where("air_time").between(startTime, startTime.plusMonths(1)));
                } else {
                    LocalDateTime startTime = Year.parse(time).atMonth(1).atDay(1).atStartOfDay();
                    criteria = criteria.and(
                        Criteria.where("air_time").between(startTime, startTime.plusYears(1)));
                }
            }
        }

        Query query = Query.query(criteria);

        if (updateTimeDesc) {
            query = query.sort(Sort.by(Sort.Order.desc("update_time")));
        }

        query = query
            .sort(Sort.by(airTimeDesc ? Sort.Order.desc("air_time")
                : Sort.Order.asc("air_time")))
            .sort(Sort.by(Sort.Order.asc("name")))
            .with(pageRequest);

        Flux<SubjectEntity> subjectEntityFlux = template.select(query, SubjectEntity.class);
        Mono<Long> countMono = template.count(query, SubjectEntity.class);

        return subjectEntityFlux.map(BaseEntity::getId)
            .flatMap(subjectRepository::findById)
            .flatMap(entity -> copyProperties(entity, new Subject()))
            .collectList()
            .flatMap(subjects -> countMono
                .map(count -> new PagingWrap<>(page, size, count, subjects)));
    }

    @Override
    @MonoCacheEvict
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
