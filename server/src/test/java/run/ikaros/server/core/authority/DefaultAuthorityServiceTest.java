package run.ikaros.server.core.authority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Query;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.authority.Authority;
import run.ikaros.api.core.authority.AuthorityCondition;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AuthorityType;
import run.ikaros.server.store.entity.AuthorityEntity;
import run.ikaros.server.store.repository.AuthorityRepository;

class DefaultAuthorityServiceTest {

    private AuthorityRepository authorityRepository;
    private R2dbcEntityTemplate template;
    private DefaultAuthorityService defaultAuthorityService;

    @BeforeEach
    void setUp() {
        authorityRepository = Mockito.mock(AuthorityRepository.class);
        template = Mockito.mock(R2dbcEntityTemplate.class);
        defaultAuthorityService =
            new DefaultAuthorityService(authorityRepository, template);
    }

    private AuthorityEntity buildAuthorityEntity(UUID id, AuthorityType type,
                                                 String target,
                                                 String authority,
                                                 Boolean allow) {
        AuthorityEntity entity = AuthorityEntity.builder()
            .type(type)
            .target(target)
            .authority(authority)
            .allow(allow)
            .build();
        if (id != null) {
            entity.setId(id);
        }
        return entity;
    }

    @Test
    void getAuthorityTypes_allValues() {
        StepVerifier.create(defaultAuthorityService.getAuthorityTypes())
            .expectNextCount(AuthorityType.values().length)
            .verifyComplete();
    }

    @Test
    void getAuthorityTypes_containsExpectedValues() {
        StepVerifier.create(defaultAuthorityService.getAuthorityTypes())
            .expectNext("ALL", "API", "APIS", "MENU", "URL", "OTHERS")
            .verifyComplete();
    }

    @Test
    void getAuthoritiesByType_found() {
        UUID id1 = UuidV7Utils.generateUuid();
        UUID id2 = UuidV7Utils.generateUuid();
        AuthorityEntity entity1 = buildAuthorityEntity(id1,
            AuthorityType.API, "/api/test", "read", true);
        AuthorityEntity entity2 = buildAuthorityEntity(id2,
            AuthorityType.API, "/api/test2", "write", true);

        when(authorityRepository.findAllByType(AuthorityType.API))
            .thenReturn(Flux.just(entity1, entity2));

        StepVerifier.create(
                defaultAuthorityService
                    .getAuthoritiesByType(AuthorityType.API))
            .assertNext(auth -> {
                assertThat(auth.getId()).isEqualTo(id1);
                assertThat(auth.getType())
                    .isEqualTo(AuthorityType.API);
                assertThat(auth.getTarget()).isEqualTo("/api/test");
                assertThat(auth.getAuthority()).isEqualTo("read");
                assertThat(auth.getAllow()).isTrue();
            })
            .assertNext(auth -> {
                assertThat(auth.getId()).isEqualTo(id2);
                assertThat(auth.getTarget()).isEqualTo("/api/test2");
            })
            .verifyComplete();
    }

    @Test
    void getAuthoritiesByType_empty() {
        when(authorityRepository.findAllByType(AuthorityType.MENU))
            .thenReturn(Flux.empty());

        StepVerifier.create(
                defaultAuthorityService
                    .getAuthoritiesByType(AuthorityType.MENU))
            .verifyComplete();
    }

    @Test
    void saveEntity_newEntity() {
        UUID id = UuidV7Utils.generateUuid();
        AuthorityEntity entity = buildAuthorityEntity(null,
            AuthorityType.API, "/api/test", "read", true);

        when(authorityRepository
            .findByTypeAndTargetAndAuthority(
                AuthorityType.API, "/api/test", "read"))
            .thenReturn(Mono.empty());
        when(authorityRepository.save(any(AuthorityEntity.class)))
            .thenAnswer(inv -> {
                AuthorityEntity e = inv.getArgument(0);
                e.setId(id);
                return Mono.just(e);
            });

        StepVerifier.create(
                defaultAuthorityService.saveEntity(entity))
            .assertNext(saved -> {
                assertThat(saved.getId()).isEqualTo(id);
                assertThat(saved.getType())
                    .isEqualTo(AuthorityType.API);
                assertThat(saved.getTarget()).isEqualTo("/api/test");
                assertThat(saved.getAuthority()).isEqualTo("read");
                assertThat(saved.getAllow()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    void saveEntity_existingEntity() {
        UUID existingId = UuidV7Utils.generateUuid();
        AuthorityEntity existingEntity = buildAuthorityEntity(existingId,
            AuthorityType.API, "/api/test", "read", false);
        AuthorityEntity updatedEntity = buildAuthorityEntity(null,
            AuthorityType.API, "/api/test", "read", true);

        when(authorityRepository
            .findByTypeAndTargetAndAuthority(
                AuthorityType.API, "/api/test", "read"))
            .thenReturn(Mono.just(existingEntity));
        when(authorityRepository.save(any(AuthorityEntity.class)))
            .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

        StepVerifier.create(
                defaultAuthorityService.saveEntity(updatedEntity))
            .assertNext(saved -> {
                assertThat(saved.getId()).isEqualTo(existingId);
                assertThat(saved.getAllow()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    void saveWithauthorityvo() {
        UUID id = UuidV7Utils.generateUuid();
        Authority authority = Authority.builder()
            .type(AuthorityType.API)
            .target("/api/users")
            .authority("read")
            .allow(true)
            .build();

        AuthorityEntity entityToSave = buildAuthorityEntity(null,
            AuthorityType.API, "/api/users", "read", true);

        when(authorityRepository
            .findByTypeAndTargetAndAuthority(
                AuthorityType.API, "/api/users", "read"))
            .thenReturn(Mono.empty());
        when(authorityRepository.save(any(AuthorityEntity.class)))
            .thenAnswer(inv -> {
                AuthorityEntity e = inv.getArgument(0);
                e.setId(id);
                return Mono.just(e);
            });

        StepVerifier.create(defaultAuthorityService.save(authority))
            .assertNext(saved -> {
                assertThat(saved.getId()).isEqualTo(id);
                assertThat(saved.getType())
                    .isEqualTo(AuthorityType.API);
                assertThat(saved.getTarget()).isEqualTo("/api/users");
                assertThat(saved.getAuthority()).isEqualTo("read");
                assertThat(saved.getAllow()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    void deleteById_success() {
        UUID id = UuidV7Utils.generateUuid();
        when(authorityRepository.deleteById(id))
            .thenReturn(Mono.empty());

        StepVerifier.create(defaultAuthorityService.deleteById(id))
            .verifyComplete();

        verify(authorityRepository).deleteById(id);
    }

    @Test
    void findAllByCondition_withResults() {
        UUID id1 = UuidV7Utils.generateUuid();
        UUID id2 = UuidV7Utils.generateUuid();
        AuthorityEntity entity1 = buildAuthorityEntity(id1,
            AuthorityType.API, "/api/test", "read", true);
        AuthorityEntity entity2 = buildAuthorityEntity(id2,
            AuthorityType.API, "/api/test2", "write", true);

        AuthorityCondition condition = AuthorityCondition.builder()
            .type(AuthorityType.API)
            .allow(true)
            .target("test")
            .authority("read")
            .page(1)
            .size(10)
            .build();

        when(template.count(any(Query.class),
            Mockito.eq(AuthorityEntity.class)))
            .thenReturn(Mono.just(2L));
        when(template.select(any(Query.class),
            Mockito.eq(AuthorityEntity.class)))
            .thenReturn(Flux.just(entity1, entity2));

        StepVerifier.create(
                defaultAuthorityService.findAllByCondition(condition))
            .assertNext(pagingWrap -> {
                assertThat(pagingWrap.getPage()).isEqualTo(1);
                assertThat(pagingWrap.getSize()).isEqualTo(10);
                assertThat(pagingWrap.getTotal()).isEqualTo(2);
                assertThat(pagingWrap.getItems()).hasSize(2);
            })
            .verifyComplete();
    }

    @Test
    void findAllByCondition_empty() {
        AuthorityCondition condition = AuthorityCondition.builder()
            .type(AuthorityType.API)
            .allow(true)
            .target("nonexistent")
            .authority("none")
            .page(1)
            .size(10)
            .build();

        when(template.count(any(Query.class),
            Mockito.eq(AuthorityEntity.class)))
            .thenReturn(Mono.just(0L));
        when(template.select(any(Query.class),
            Mockito.eq(AuthorityEntity.class)))
            .thenReturn(Flux.empty());

        StepVerifier.create(
                defaultAuthorityService.findAllByCondition(condition))
            .assertNext(pagingWrap -> {
                assertThat(pagingWrap.getPage()).isEqualTo(1);
                assertThat(pagingWrap.getSize()).isEqualTo(10);
                assertThat(pagingWrap.getTotal()).isEqualTo(0);
                assertThat(pagingWrap.getItems()).isEmpty();
            })
            .verifyComplete();
    }
}
