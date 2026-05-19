package run.ikaros.server.core.attachment.service;


import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.core.attachment.vo.PostAttachmentRelationsParam;
import run.ikaros.server.store.entity.AttachmentRelationEntity;
import run.ikaros.server.store.repository.AttachmentRelationRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class AttachmentRelationServiceTest {

    @Autowired
    AttachmentRelationService attachmentRelationService;
    @Autowired
    AttachmentRelationRepository attachmentRelationRepository;
    @Autowired
    AttachmentRepository attachmentRepository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(attachmentRelationRepository.deleteAll()).verifyComplete();
        StepVerifier.create(attachmentRepository.deleteAll()).verifyComplete();
    }

    @Test
    void putAttachmentRelation() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationAttachmentId = UuidV7Utils.generateUuid();

        AttachmentRelation attachmentRelation = AttachmentRelation.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationAttachmentId)
            .build();

        StepVerifier.create(attachmentRelationService.putAttachmentRelation(attachmentRelation))
            .expectNextMatches(rel ->
                rel.getAttachmentId().equals(attachmentId)
                    && rel.getRelationAttachmentId().equals(relationAttachmentId)
                    && rel.getType().equals(AttachmentRelationType.VIDEO_SUBTITLE))
            .verifyComplete();
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

        StepVerifier.create(attachmentRelationRepository.insert(entity1))
            .expectNext(entity1).verifyComplete();
        StepVerifier.create(attachmentRelationRepository.insert(entity2))
            .expectNext(entity2).verifyComplete();

        StepVerifier.create(attachmentRelationService.findAllByTypeAndAttachmentId(
                AttachmentRelationType.VIDEO_SUBTITLE, attachmentId))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void deleteAttachmentRelation() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationAttachmentId = UuidV7Utils.generateUuid();

        AttachmentRelationEntity entity = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationAttachmentId)
            .build();

        StepVerifier.create(attachmentRelationRepository.insert(entity))
            .expectNext(entity).verifyComplete();

        AttachmentRelation attachmentRelation = AttachmentRelation.builder()
            .id(entity.getId())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationAttachmentId)
            .build();

        StepVerifier.create(attachmentRelationService.deleteAttachmentRelation(attachmentRelation))
            .expectNext(attachmentRelation).verifyComplete();

        StepVerifier.create(
                attachmentRelationRepository.existsByTypeAndAttachmentIdAndRelationAttachmentId(
                    AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationAttachmentId))
            .expectNext(false).verifyComplete();
    }

    @Test
    void putAttachmentRelations() {
        UUID masterId = UuidV7Utils.generateUuid();
        UUID relationId1 = UuidV7Utils.generateUuid();
        UUID relationId2 = UuidV7Utils.generateUuid();

        PostAttachmentRelationsParam param = new PostAttachmentRelationsParam();
        param.setMasterId(masterId);
        param.setType(AttachmentRelationType.VIDEO_SUBTITLE);
        param.setRelationIds(List.of(relationId1, relationId2));

        StepVerifier.create(attachmentRelationService.putAttachmentRelations(param))
            .expectNextCount(2).verifyComplete();

        StepVerifier.create(attachmentRelationRepository.findAllByTypeAndAttachmentId(
                AttachmentRelationType.VIDEO_SUBTITLE, masterId))
            .expectNextCount(2).verifyComplete();
    }
}
