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
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class AttachmentReferenceRepositoryTest {

    @Autowired
    AttachmentReferenceRepository repository;


    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void findAllByType() {
        UUID attId1 = UuidV7Utils.generateUuid();
        UUID refId1 = UuidV7Utils.generateUuid();

        AttachmentReferenceEntity attRef1 = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attId1)
            .referenceId(refId1)
            .build();
        StepVerifier.create(repository.insert(attRef1))
            .expectNext(attRef1)
            .verifyComplete();

        StepVerifier.create(repository.findAllByTypeAndAttachmentId(
                AttachmentReferenceType.EPISODE, attId1))
            .expectNext(attRef1)
            .verifyComplete();

        StepVerifier.create(repository.findAllByTypeAndReferenceId(
                AttachmentReferenceType.EPISODE, refId1))
            .expectNext(attRef1)
            .verifyComplete();

    }

    @Test
    void existsByTypeAndAttachmentId() {
        UUID attId = UuidV7Utils.generateUuid();
        UUID refId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.existsByTypeAndAttachmentId(
                AttachmentReferenceType.EPISODE, attId))
            .expectNext(false).verifyComplete();

        AttachmentReferenceEntity entity = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attId)
            .referenceId(refId)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByTypeAndAttachmentId(
                AttachmentReferenceType.EPISODE, attId))
            .expectNext(true).verifyComplete();
    }

    @Test
    void existsByTypeAndReferenceId() {
        UUID attId = UuidV7Utils.generateUuid();
        UUID refId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.existsByTypeAndReferenceId(
                AttachmentReferenceType.EPISODE, refId))
            .expectNext(false).verifyComplete();

        AttachmentReferenceEntity entity = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attId)
            .referenceId(refId)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByTypeAndReferenceId(
                AttachmentReferenceType.EPISODE, refId))
            .expectNext(true).verifyComplete();
    }

    @Test
    void existsByAttachmentId() {
        UUID attId = UuidV7Utils.generateUuid();
        UUID refId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.existsByAttachmentId(attId))
            .expectNext(false).verifyComplete();

        AttachmentReferenceEntity entity = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attId)
            .referenceId(refId)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByAttachmentId(attId))
            .expectNext(true).verifyComplete();
    }

    @Test
    void countByTypeAndReferenceId() {
        UUID refId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.countByTypeAndReferenceId(
                AttachmentReferenceType.EPISODE, refId))
            .expectNext(0L).verifyComplete();

        AttachmentReferenceEntity entity1 = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(UuidV7Utils.generateUuid())
            .referenceId(refId)
            .build();

        AttachmentReferenceEntity entity2 = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(UuidV7Utils.generateUuid())
            .referenceId(refId)
            .build();

        StepVerifier.create(repository.insert(entity1))
            .expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2))
            .expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.countByTypeAndReferenceId(
                AttachmentReferenceType.EPISODE, refId))
            .expectNext(2L).verifyComplete();
    }
}