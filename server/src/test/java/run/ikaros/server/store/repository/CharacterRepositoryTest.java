package run.ikaros.server.store.repository;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.CharacterEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class CharacterRepositoryTest {

    @Autowired
    CharacterRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        CharacterEntity entity = CharacterEntity.builder()
            .name("Test Character")
            .infobox("infobox content")
            .summary("summary content")
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findByName() {
        String name = "Findable Character-" + UuidV7Utils.generate().substring(0, 8);

        CharacterEntity entity = CharacterEntity.builder()
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
}
