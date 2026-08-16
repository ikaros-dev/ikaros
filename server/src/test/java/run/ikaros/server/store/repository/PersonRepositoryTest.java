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
import run.ikaros.server.store.entity.PersonEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class PersonRepositoryTest {

    @Autowired
    PersonRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        PersonEntity entity = PersonEntity.builder()
            .name("Test Person")
            .infobox("infobox content")
            .summary("summary content")
            .build();
        UUID entityId = UuidV7Utils.generateUuid();
        entity.setId(entityId);

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entityId))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findByName() {
        String name = "Findable Person-" + UuidV7Utils.generate().substring(0, 8);

        PersonEntity entity = PersonEntity.builder()
            .name(name)
            .infobox("infobox")
            .summary("summary")
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findByName(name))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void countActiveExcludesLogicallyDeletedPeople() {
        PersonEntity active = PersonEntity.builder().name("active").build();
        PersonEntity deleted = PersonEntity.builder().name("deleted").build();
        active.setId(UuidV7Utils.generateUuid());
        active.setDeleteStatus(false);
        deleted.setId(UuidV7Utils.generateUuid());
        deleted.setDeleteStatus(true);

        StepVerifier.create(repository.insert(active)
                .then(repository.insert(deleted))
                .then(repository.countActive()))
            .expectNext(1L)
            .verifyComplete();
    }
}
