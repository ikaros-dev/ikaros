package run.ikaros.server.store.repository;


import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.RoleEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class RoleRepositoryTest {

    @Autowired
    RoleRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        RoleEntity entity = RoleEntity.builder()
            .name("test-role-" + UuidV7Utils.generate().substring(0, 8))
            .description("Test role description")
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findByName() {
        String roleName = "findable-role-" + UuidV7Utils.generate().substring(0, 8);

        RoleEntity entity = RoleEntity.builder()
            .name(roleName)
            .description("Test role")
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findByName(roleName))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void existsByName() {
        String roleName = "checkable-role-" + UuidV7Utils.generate().substring(0, 8);

        StepVerifier.create(repository.existsByName(roleName))
            .expectNext(false).verifyComplete();

        RoleEntity entity = RoleEntity.builder()
            .name(roleName)
            .description("Test role")
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByName(roleName))
            .expectNext(true).verifyComplete();
    }
}
