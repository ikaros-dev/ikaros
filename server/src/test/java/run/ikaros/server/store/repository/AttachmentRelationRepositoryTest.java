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
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.AttachmentRelationEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class AttachmentRelationRepositoryTest {

    @Autowired
    AttachmentRelationRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void insert() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationAttachmentId = UuidV7Utils.generateUuid();

        AttachmentRelationEntity entity = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationAttachmentId)
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findAllByTypeAndAttachmentId() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationId1 = UuidV7Utils.generateUuid();
        UUID relationId2 = UuidV7Utils.generateUuid();

        AttachmentRelationEntity entity1 = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationId1)
            .build();

        AttachmentRelationEntity entity2 = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationId2)
            .build();

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllByTypeAndAttachmentId(
                AttachmentRelationType.VIDEO_SUBTITLE, attachmentId))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void existsByTypeAndAttachmentIdAndRelationAttachmentId() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(
                AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationId))
            .expectNext(false).verifyComplete();

        AttachmentRelationEntity entity = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationId)
            .build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(
                AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationId))
            .expectNext(true).verifyComplete();
    }

    @Test
    void deleteByTypeAndAttachmentIdAndRelationAttachmentId() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationId = UuidV7Utils.generateUuid();

        AttachmentRelationEntity entity = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationId)
            .build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.deleteByTypeAndAttachmentIdAndRelationAttachmentId(
                AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationId))
            .verifyComplete();

        StepVerifier.create(repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(
                AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationId))
            .expectNext(false).verifyComplete();
    }

    @Test
    void findAllByAttachmentId() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationId = UuidV7Utils.generateUuid();

        AttachmentRelationEntity entity = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationId)
            .build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findAllByAttachmentId(attachmentId))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void existsByAttachmentId() {
        UUID attachmentId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.existsByAttachmentId(attachmentId))
            .expectNext(false).verifyComplete();

        AttachmentRelationEntity entity = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(UuidV7Utils.generateUuid())
            .build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        StepVerifier.create(repository.existsByAttachmentId(attachmentId))
            .expectNext(true).verifyComplete();
    }

    @Test
    void deleteAllByAttachmentId() {
        UUID attachmentId = UuidV7Utils.generateUuid();

        AttachmentRelationEntity entity1 = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(UuidV7Utils.generateUuid())
            .build();

        AttachmentRelationEntity entity2 = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(UuidV7Utils.generateUuid())
            .build();

        StepVerifier.create(repository.insert(entity1)).expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2)).expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.deleteAllByAttachmentId(attachmentId))
            .verifyComplete();

        StepVerifier.create(repository.findAllByAttachmentId(attachmentId))
            .expectNextCount(0).verifyComplete();
    }
}
