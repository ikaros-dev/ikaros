package run.ikaros.server.core.tag;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.tag.SubjectTag;
import run.ikaros.api.core.tag.Tag;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.store.entity.TagEntity;
import run.ikaros.server.store.repository.TagRepository;

@Slf4j
@Service
public class DefaultTagService implements TagService {
    private final TagRepository tagRepository;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    public DefaultTagService(TagRepository tagRepository, R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.tagRepository = tagRepository;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    @Override
    public Flux<Tag> findAll(TagType type, Long masterId, Long userId, String name) {
        Criteria criteria = Criteria.empty();

        if (Objects.nonNull(type)) {
            criteria = criteria.and(Criteria.where("type").is(type));
        }

        if (Objects.nonNull(masterId) && masterId >= 0) {
            criteria = criteria.and("master_id").is(masterId);
        }

        if (Objects.nonNull(userId) && userId >= 0) {
            criteria = criteria.and("user_id").is(userId);
        }

        if (StringUtils.isNotBlank(name)) {
            criteria = criteria.and("name").like("%" + name + "%");
        }

        Query query = Query.query(criteria).sort(Sort.by(Sort.Order.desc("create_time")));

        return r2dbcEntityTemplate.select(query, TagEntity.class)
            .flatMap(tagEntity -> copyProperties(tagEntity, new Tag()));
    }

    @Override
    public Flux<SubjectTag> findSubjectTags(Long subjectId) {
        Assert.isTrue(subjectId >= 0, "'subjectId' must >=0.");
        return findAll(TagType.SUBJECT, subjectId, null, null)
            .map(tag -> SubjectTag.builder()
                .id(tag.getId())
                .subjectId(tag.getMasterId())
                .userId(tag.getUserId())
                .name(tag.getName())
                .createTime(tag.getCreateTime())
                .build());
    }

    @Override
    public Mono<Tag> create(Tag tag) {
        Assert.isNull(tag.getId(), "'tag id' must is null.");
        Assert.notNull(tag.getType(), "'type' must not null.");
        Assert.isTrue(tag.getMasterId() >= 0, "'masterId' must >=0.");
        Assert.hasText(tag.getName(), "'name' must has text.");
        return Mono.just(tag)
            .flatMap(tag1 -> copyProperties(tag1, new TagEntity()))
            .flatMap(tagRepository::save)
            .flatMap(tagEntity -> copyProperties(tagEntity, tag));
    }

    @Override
    public Mono<Void> remove(TagType type, Long masterId, String name) {
        Assert.notNull(type, "'type' must not null.");
        Assert.isTrue(masterId >= 0, "'masterId' must >=0.");
        Assert.hasText(name, "'name' must has text.");
        return findAll(type, masterId, null, name)
            .flatMap(tag -> removeById(tag.getId()))
            .then();
    }

    @Override
    public Mono<Void> removeById(Long tagId) {
        Assert.isTrue(tagId >= 0, "'tagId' must >=0.");
        return tagRepository.findById(tagId)
            .switchIfEmpty(Mono.error(new NotFoundException("Tag not found for id = " + tagId)))
            .map(TagEntity::getId)
            .flatMap(tagRepository::deleteById);
    }
}
