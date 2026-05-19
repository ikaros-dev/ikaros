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
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.SubjectRelationEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class SubjectRelationRepositoryTest {

    @Autowired
    SubjectRelationRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        UUID subjectId = UuidV7Utils.generateUuid();
        UUID relationSubjectId = UuidV7Utils.generateUuid();

        SubjectRelationEntity entity = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjectId(relationSubjectId)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findAllBySubjectIdAndRelationType() {
        UUID subjectId = UuidV7Utils.generateUuid();
        UUID relationSubjectId1 = UuidV7Utils.generateUuid();
        UUID relationSubjectId2 = UuidV7Utils.generateUuid();

        SubjectRelationEntity entity1 = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjectId(relationSubjectId1)
            .build();
        entity1.setId(UuidV7Utils.generateUuid());

        SubjectRelationEntity entity2 = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjectId(relationSubjectId2)
            .build();
        entity2.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllBySubjectIdAndRelationType(
                subjectId, SubjectRelationType.COMIC))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void findAllBySubjectId() {
        UUID subjectId = UuidV7Utils.generateUuid();

        SubjectRelationEntity entity1 = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjectId(UuidV7Utils.generateUuid())
            .build();
        entity1.setId(UuidV7Utils.generateUuid());

        SubjectRelationEntity entity2 = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.ANIME)
            .relationSubjectId(UuidV7Utils.generateUuid())
            .build();
        entity2.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllBySubjectId(subjectId))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void deleteBySubjectIdAndRelationTypeAndRelationSubjectId() {
        UUID subjectId = UuidV7Utils.generateUuid();
        UUID relationSubjectId = UuidV7Utils.generateUuid();

        SubjectRelationEntity entity = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjectId(relationSubjectId)
            .build();
        entity.setId(UuidV7Utils.generateUuid());

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.deleteBySubjectIdAndRelationTypeAndRelationSubjectId(
                subjectId, SubjectRelationType.COMIC, relationSubjectId))
            .verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNextCount(0).verifyComplete();
    }
}
