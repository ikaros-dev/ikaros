package run.ikaros.server.core.attachment.service.impl;

import static run.ikaros.server.infra.utils.ReactiveBeanUtils.copyProperties;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

@Slf4j
@Service
public class AttachmentServiceImpl implements AttachmentService {
    private final AttachmentRepository repository;
    private final R2dbcEntityTemplate template;

    public AttachmentServiceImpl(AttachmentRepository repository,
                                 R2dbcEntityTemplate template) {
        this.repository = repository;
        this.template = template;
    }

    @Override
    public Mono<AttachmentEntity> saveEntity(AttachmentEntity attachmentEntity) {
        Assert.notNull(attachmentEntity, "'attachmentEntity' must not be null.");
        return repository.save(attachmentEntity.setUpdateTime(LocalDateTime.now()));
    }

    @Override
    public Mono<Attachment> save(Attachment attachment) {
        Assert.notNull(attachment, "'attachment' must not be null.");
        return repository.findById(attachment.getId())
            .flatMap(attachmentEntity -> copyProperties(attachment, attachmentEntity))
            .flatMap(this::saveEntity)
            .flatMap(attachmentEntity -> copyProperties(attachmentEntity, attachment));
    }

    @Override
    public Mono<PagingWrap<AttachmentEntity>> listEntitiesByCondition(
        AttachmentSearchCondition condition) {
        Assert.notNull(condition, "'condition' must no null.");

        final Integer page = condition.getPage();
        Assert.isTrue(page > 0, "'page' must gt 0.");

        final Integer size = condition.getSize();
        Assert.isTrue(size > 0, "'size' must gt 0.");

        final String name = StringUtils.hasText(condition.getName())
            ? condition.getName() : "";
        final String nameLike = "%" + name + "%";
        final AttachmentType type = condition.getType();
        final Long parentId = condition.getParentId();

        PageRequest pageRequest = PageRequest.of(page - 1, size);

        Criteria criteria = Criteria.where("parent_id").is(parentId);

        if (Objects.nonNull(type)) {
            criteria = criteria.and("type").is(type);
        }

        if (!StringUtils.hasText(name)) {
            criteria = criteria.and("name").like(nameLike);
        }

        Query query = Query.query(criteria).with(pageRequest);

        Flux<AttachmentEntity> attachmentEntityFlux =
            template.select(query, AttachmentEntity.class);
        Mono<Long> countMono = template.count(query, AttachmentEntity.class);

        return countMono.flatMap(total -> attachmentEntityFlux.collectList()
            .map(attachmentEntities -> new PagingWrap<>(page, size, total, attachmentEntities)));
    }

    @Override
    public Mono<PagingWrap<Attachment>> listByCondition(AttachmentSearchCondition condition) {
        Assert.notNull(condition, "'condition' must no null.");
        return listEntitiesByCondition(condition)
            .flatMap(pagingWrap -> Flux.fromStream(pagingWrap.getItems().stream())
                .flatMap(attachmentEntity -> copyProperties(attachmentEntity, new Attachment()))
                .collectList()
                .map(attachments -> new PagingWrap<>(pagingWrap.getPage(), pagingWrap.getSize(),
                    pagingWrap.getTotal(), attachments)));
    }
}
