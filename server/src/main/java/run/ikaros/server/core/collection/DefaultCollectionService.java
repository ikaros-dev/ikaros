package run.ikaros.server.core.collection;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.collection.EpisodeCollection;
import run.ikaros.api.core.collection.SubjectCollection;
import run.ikaros.api.core.collection.vo.FindCollectionCondition;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.enums.CollectionCategory;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.store.entity.EpisodeCollectionEntity;
import run.ikaros.server.store.entity.SubjectCollectionEntity;
import run.ikaros.server.store.repository.EpisodeCollectionRepository;
import run.ikaros.server.store.repository.SubjectCollectionRepository;

@Slf4j
@Service
public class DefaultCollectionService implements CollectionService {
    private final SubjectCollectionRepository subjectCollectionRepository;
    private final EpisodeCollectionRepository episodeCollectionRepository;

    private final UserService userService;
    private final R2dbcEntityTemplate template;

    /**
     * Construct a {@link CollectionService} instance.
     */
    public DefaultCollectionService(SubjectCollectionRepository subjectCollectionRepository,
                                    EpisodeCollectionRepository episodeCollectionRepository,
                                    UserService userService, R2dbcEntityTemplate template) {
        this.subjectCollectionRepository = subjectCollectionRepository;
        this.episodeCollectionRepository = episodeCollectionRepository;
        this.userService = userService;
        this.template = template;
    }

    @Override
    public Mono<CollectionType> findTypeBySubjectId(Long subjectId) {
        Assert.isTrue(subjectId > 0, "subjectId must be greater than 0");
        return userService.getUserIdFromSecurityContext()
            .flatMap(uid -> subjectCollectionRepository.findByUserIdAndSubjectId(uid, subjectId))
            .map(SubjectCollectionEntity::getType);
    }

    @Override
    public Mono<PagingWrap> listCollectionsByCondition(FindCollectionCondition condition) {
        Assert.notNull(condition, "'condition' must not null.");
        final Integer page = condition.getPage();
        Assert.isTrue(page > 0, "'page' must gt 0.");
        final Integer size = condition.getSize();
        Assert.isTrue(size > 0, "'size' must gt 0.");
        final PageRequest pageRequest = PageRequest.of(page - 1, size);

        Criteria criteria = Criteria.empty();

        if (CollectionCategory.SUBJECT == condition.getCategory()
            && !Objects.isNull(condition.getType())) {
            criteria = criteria.and("type").is(condition.getType());
        }

        // 范围查询更新时间，用于查询指定时间段的收藏纪录
        String time = condition.getTime();
        if (CollectionCategory.EPISODE == condition.getCategory()
            && StringUtils.isNotBlank(time)
        ) {
            String[] split = time.split("-");
            String first = split[0];
            String second = split[1];
            LocalDateTime startTime = Instant.ofEpochMilli(Long.parseLong(first))
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            LocalDateTime endTime = Instant.ofEpochMilli(Long.parseLong(second))
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            criteria = criteria.and(Criteria.where("update_time").between(startTime, endTime));
        }

        Query query = Query.query(criteria);

        if (CollectionCategory.EPISODE == condition.getCategory()
            && condition.getUpdateTimeDesc()) {
            query = query.sort(Sort.by(Sort.Order.desc("update_time").nullsLast()));
        }

        if (CollectionCategory.EPISODE == condition.getCategory()) {
            query = query
                .sort(Sort.by(
                    condition.getUpdateTimeDesc() ? Sort.Order.desc("update_time").nullsLast()
                        : Sort.Order.asc("update_time").nullsLast()));
        }

        query = query.with(pageRequest);

        Mono<Long> countMono;
        if (CollectionCategory.EPISODE == condition.getCategory()) {
            Flux<EpisodeCollectionEntity> episodeCollectionEntityFlux =
                template.select(query, EpisodeCollectionEntity.class);
            countMono = template.count(query, EpisodeCollectionEntity.class);
            return episodeCollectionEntityFlux.map(EpisodeCollectionEntity::getId)
                .flatMap(episodeCollectionRepository::findById)
                .flatMap(entity -> copyProperties(entity, new EpisodeCollection()))
                .collectList()
                .flatMap(episodeCollections -> countMono
                    .map(count -> new PagingWrap<>(page, size, count, episodeCollections)));
        } else if (CollectionCategory.SUBJECT == condition.getCategory()) {
            Flux<SubjectCollectionEntity> subjectCollectionEntityFlux =
                template.select(query, SubjectCollectionEntity.class);
            countMono = template.count(query, SubjectCollectionEntity.class);
            return subjectCollectionEntityFlux.map(SubjectCollectionEntity::getId)
                .flatMap(subjectCollectionRepository::findById)
                .flatMap(entity -> copyProperties(entity, new SubjectCollection()))
                .collectList()
                .flatMap(subjects -> countMono
                    .map(count -> new PagingWrap<>(page, size, count, subjects)));
        } else {
            return Mono.empty();
        }
    }
}
