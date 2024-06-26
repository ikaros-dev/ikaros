package run.ikaros.server.core.authority;

import static run.ikaros.api.infra.utils.ReactiveBeanUtils.copyProperties;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import run.ikaros.api.security.Authority;
import run.ikaros.api.store.enums.AuthorityType;
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
}
