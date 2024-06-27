package run.ikaros.server.core.authority;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.security.Authority;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.store.entity.AuthorityEntity;
import run.ikaros.server.store.repository.AuthorityRepository;

@Slf4j
@Service
public class DefaultAuthorityService implements AuthorityService {
    private final AuthorityRepository authorityRepository;

    public DefaultAuthorityService(AuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
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
            .flatMap(entity -> copyProperties(entity, new Authority()));
    }

    @Override
    public Mono<AuthorityEntity> saveEntity(AuthorityEntity entity) {
        Assert.notNull(entity, "entity must not be null");
        Assert.notNull(entity.getType(), "type must not be null");
        Assert.hasText(entity.getTarget(), "target must has text");
        Assert.hasText(entity.getAuthority(), "authority must has text");
        return authorityRepository.findByTypeAndTargetAndAuthority(
                entity.getType(), entity.getTarget(), entity.getAuthority())
            .switchIfEmpty(Mono.just(entity))
            .flatMap(e -> copyProperties(entity, e))
            .flatMap(authorityRepository::save);
    }
}
