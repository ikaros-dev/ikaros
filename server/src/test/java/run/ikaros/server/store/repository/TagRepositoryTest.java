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
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.TagEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class TagRepositoryTest {

    @Autowired
    TagRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        UUID masterId = UuidV7Utils.generateUuid();
        UUID userId = UuidV7Utils.generateUuid();

        TagEntity entity = TagEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(TagType.SUBJECT)
            .masterId(masterId)
            .name("test-tag")
            .userId(userId)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findAllByTypeAndMasterId() {
        UUID masterId = UuidV7Utils.generateUuid();
        UUID userId = UuidV7Utils.generateUuid();

        TagEntity entity1 = TagEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(TagType.SUBJECT)
            .masterId(masterId)
            .name("tag-1")
            .userId(userId)
            .build();

        TagEntity entity2 = TagEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(TagType.SUBJECT)
            .masterId(masterId)
            .name("tag-2")
            .userId(userId)
            .build();

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllByTypeAndMasterId(TagType.SUBJECT, masterId))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void findByTypeAndMasterIdAndName() {
        UUID masterId = UuidV7Utils.generateUuid();
        UUID userId = UuidV7Utils.generateUuid();

        TagEntity entity = TagEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(TagType.SUBJECT)
            .masterId(masterId)
            .name("unique-tag")
            .userId(userId)
            .build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findByTypeAndMasterIdAndName(
                TagType.SUBJECT, masterId, "unique-tag"))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void existsByTypeAndMasterIdAndName() {
        UUID masterId = UuidV7Utils.generateUuid();
        UUID userId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.existsByTypeAndMasterIdAndName(
                TagType.SUBJECT, masterId, "nonexistent"))
            .expectNext(false).verifyComplete();

        TagEntity entity = TagEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(TagType.SUBJECT)
            .masterId(masterId)
            .name("existing-tag")
            .userId(userId)
            .build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByTypeAndMasterIdAndName(
                TagType.SUBJECT, masterId, "existing-tag"))
            .expectNext(true).verifyComplete();
    }

    @Test
    void existsByTypeAndUserIdAndName() {
        UUID userId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.existsByTypeAndUserIdAndName(
                TagType.SUBJECT, userId, "nonexistent"))
            .expectNext(false).verifyComplete();

        TagEntity entity = TagEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(TagType.SUBJECT)
            .masterId(UuidV7Utils.generateUuid())
            .name("user-tag")
            .userId(userId)
            .build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByTypeAndUserIdAndName(
                TagType.SUBJECT, userId, "user-tag"))
            .expectNext(true).verifyComplete();
    }
}
