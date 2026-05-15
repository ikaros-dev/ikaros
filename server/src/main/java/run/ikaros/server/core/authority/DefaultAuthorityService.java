package run.ikaros.server.core.authority;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.authority.Authority;
import run.ikaros.api.core.authority.AuthorityCondition;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.AuthorityEntity;
import run.ikaros.server.store.repository.AuthorityRepository;

@Slf4j
@Service
public class DefaultAuthorityService implements AuthorityService {
    private final AuthorityRepository authorityRepository;
    private final R2dbcEntityTemplate template;

    public DefaultAuthorityService(AuthorityRepository authorityRepository,
                                   R2dbcEntityTemplate template) {
        this.authorityRepository = authorityRepository;
        this.template = template;
    }

    @Override
    public Flux<String> getAuthorityTypes() {
        return Flux.fromArray(AuthorityType.values())
            .map(AuthorityType::name);
    }

    @Override
    public Flux<Authority> getAuthoritiesByType(AuthorityType type) {
        Assert.notNull(type, "type must not be null");
        return authorityRepository.findAllByType(type)
            .map(this::entity2Vo);
    }

    @Override
    public Mono<AuthorityEntity> saveEntity(AuthorityEntity entity) {
        Assert.notNull(entity, "entity must not be null");
        Assert.notNull(entity.getType(), "type must not be null");
        Assert.hasText(entity.getTarget(), "target must has text");
        Assert.hasText(entity.getAuthority(), "authority must has text");
        return authorityRepository.findByTypeAndTargetAndAuthority(
                entity.getType(), entity.getTarget(), entity.getAuthority())
            .map(e -> {
                e.setAllow(entity.getAllow());
                e.setType(entity.getType());
                e.setTarget(entity.getTarget());
                e.setAuthority(entity.getAuthority());
                return e;
            })
            .flatMap(authorityRepository::save)
            .switchIfEmpty(authorityRepository.save(entity));
    }

    @Override
    public Mono<Authority> save(Authority authority) {
        Assert.notNull(authority, "authority must not be null");
        Assert.notNull(authority.getType(), "type must not be null");
        Assert.hasText(authority.getTarget(), "target must has text");
        Assert.hasText(authority.getAuthority(), "authority must has text");
        return saveEntity(vo2Entity(authority))
            .map(this::entity2Vo);
    }

    @Override
    public Mono<Void> deleteById(UUID id) {
        return authorityRepository.deleteById(id);
    }

    @Override
    public Mono<PagingWrap<Authority>> findAllByCondition(AuthorityCondition authorityCondition) {
        Assert.notNull(authorityCondition, "authority must not be null");

        Integer page = authorityCondition.getPage();
        Integer size = authorityCondition.getSize();
        Assert.isTrue(page >= 0, "page must not be negative");
        Assert.isTrue(size >= 0, "size must not be negative");

        final AuthorityType type = authorityCondition.getType();
        Assert.notNull(type, "type must not be null");

        final Boolean allow = authorityCondition.getAllow();
        Assert.notNull(allow, "allow must not be null");

        final String target = authorityCondition.getTarget();
        final String targetLike = '%' + target + '%';

        final String authority = authorityCondition.getAuthority();
        final String authorityLike = '%' + authority + '%';

        Criteria criteria = Criteria.empty();

        criteria = criteria.and("type").is(type);
        criteria = criteria.and("allow").is(allow);


        if (StringUtils.isNotBlank(target)) {
            criteria = criteria.and("target").like(targetLike);
        }

        if (StringUtils.isNotBlank(authority)) {
            criteria = criteria.and("authority").like(authorityLike);
        }

        Query query = Query.query(criteria)
            .with(PageRequest.of(page - 1, size));

        Mono<Long> countMono = template.count(query, AuthorityEntity.class);

        return template.select(query, AuthorityEntity.class)
            .map(this::entity2Vo)
            .collectList()
            .flatMap(authorities -> countMono
                .map(count -> new PagingWrap<>(page, size, count, authorities)));
    }

    private AuthorityEntity vo2Entity(Authority authority) {
        AuthorityEntity entity = new AuthorityEntity();
        entity.setId(authority.getId());
        entity.setAllow(authority.getAllow());
        entity.setType(authority.getType());
        entity.setTarget(authority.getTarget());
        entity.setAuthority(authority.getAuthority());
        return entity;
    }

    private Authority entity2Vo(AuthorityEntity entity) {
        Authority authority = new Authority();
        authority.setId(entity.getId());
        authority.setAllow(entity.getAllow());
        authority.setType(entity.getType());
        authority.setTarget(entity.getTarget());
        authority.setAuthority(entity.getAuthority());
        return authority;
    }
}
