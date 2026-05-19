package run.ikaros.server.store.repository;


import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.CollectionType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.SubjectCollectionEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class SubjectCollectionRepositoryTest {

    @Autowired
    SubjectCollectionRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();

        SubjectCollectionEntity entity = SubjectCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(userId)
            .subjectId(subjectId)
            .type(CollectionType.WISH)
            .isPrivate(false)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findByUserIdAndSubjectId() {
        UUID userId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();

        SubjectCollectionEntity entity = SubjectCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(userId)
            .subjectId(subjectId)
            .type(CollectionType.WISH)
            .isPrivate(false)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findByUserIdAndSubjectId(userId, subjectId))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findAllByUserId() {
        UUID userId = UuidV7Utils.generateUuid();

        SubjectCollectionEntity entity1 = SubjectCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(userId)
            .subjectId(UuidV7Utils.generateUuid())
            .type(CollectionType.WISH)
            .isPrivate(false)
            .build();

        SubjectCollectionEntity entity2 = SubjectCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(userId)
            .subjectId(UuidV7Utils.generateUuid())
            .type(CollectionType.WISH)
            .isPrivate(false)
            .build();

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllByUserId(userId, PageRequest.of(0, 10)))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void countAllByUserId() {
        UUID userId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.countAllByUserId(userId))
            .expectNext(0L).verifyComplete();

        SubjectCollectionEntity entity = SubjectCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(userId)
            .subjectId(UuidV7Utils.generateUuid())
            .type(CollectionType.WISH)
            .isPrivate(false)
            .build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.countAllByUserId(userId))
            .expectNext(1L).verifyComplete();
    }

    @Test
    void countByType() {
        StepVerifier.create(repository.countByType(CollectionType.WISH))
            .expectNext(0L).verifyComplete();

        SubjectCollectionEntity entity = SubjectCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(UuidV7Utils.generateUuid())
            .subjectId(UuidV7Utils.generateUuid())
            .type(CollectionType.WISH)
            .isPrivate(false)
            .build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.countByType(CollectionType.WISH))
            .expectNext(1L).verifyComplete();
    }

    @Test
    void removeAllBySubjectId() {
        UUID subjectId = UuidV7Utils.generateUuid();

        SubjectCollectionEntity entity = SubjectCollectionEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .userId(UuidV7Utils.generateUuid())
            .subjectId(subjectId)
            .type(CollectionType.WISH)
            .isPrivate(false)
            .build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.removeAllBySubjectId(subjectId))
            .verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNextCount(0).verifyComplete();
    }
}
