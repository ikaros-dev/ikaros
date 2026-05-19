package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.server.core.attachment.vo.PostAttachmentRelationsParam;
import run.ikaros.server.store.entity.AttachmentRelationEntity;
import run.ikaros.server.store.repository.AttachmentRelationRepository;
import run.ikaros.server.store.repository.AttachmentRepository;

class AttachmentRelationServiceImplTest {

    @Mock
    private AttachmentRelationRepository repository;
    @Mock
    private AttachmentRepository attachmentRepository;
    private AttachmentRelationServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AttachmentRelationServiceImpl(repository, attachmentRepository);
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

        when(repository.findAllByTypeAndAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, attachmentId))
            .thenReturn(Flux.just(entity1, entity2));

        StepVerifier.create(service.findAllByTypeAndAttachmentId(
                AttachmentRelationType.VIDEO_SUBTITLE, attachmentId))
            .assertNext(rel -> {
                assertThat(rel.getAttachmentId()).isEqualTo(attachmentId);
                assertThat(rel.getRelationAttachmentId()).isEqualTo(relationId1);
                assertThat(rel.getType()).isEqualTo(AttachmentRelationType.VIDEO_SUBTITLE);
            })
            .assertNext(rel -> {
                assertThat(rel.getAttachmentId()).isEqualTo(attachmentId);
                assertThat(rel.getRelationAttachmentId()).isEqualTo(relationId2);
                assertThat(rel.getType()).isEqualTo(AttachmentRelationType.VIDEO_SUBTITLE);
            })
            .verifyComplete();
    }

    @Test
    void findAllByTypeAndAttachmentId_nullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.findAllByTypeAndAttachmentId(null, UuidV7Utils.generateUuid()));
    }

    @Test
    void putAttachmentRelation_insertsNewWhenNotExists() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationAttachmentId = UuidV7Utils.generateUuid();

        AttachmentRelation attachmentRelation = AttachmentRelation.builder()
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationAttachmentId)
            .build();

        AttachmentRelationEntity insertedEntity = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationAttachmentId)
            .build();

        when(repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationAttachmentId))
            .thenReturn(Mono.just(false));
        doReturn(Mono.just(insertedEntity)).when(repository)
            .insert(any(AttachmentRelationEntity.class));

        StepVerifier.create(service.putAttachmentRelation(attachmentRelation))
            .assertNext(rel -> {
                assertThat(rel.getAttachmentId()).isEqualTo(attachmentId);
                assertThat(rel.getRelationAttachmentId()).isEqualTo(relationAttachmentId);
                assertThat(rel.getType()).isEqualTo(AttachmentRelationType.VIDEO_SUBTITLE);
            })
            .verifyComplete();
    }

    @Test
    void putAttachmentRelation_returnsExistingWhenExists() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationAttachmentId = UuidV7Utils.generateUuid();

        AttachmentRelation attachmentRelation = AttachmentRelation.builder()
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationAttachmentId)
            .build();

        AttachmentRelationEntity existingEntity = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationAttachmentId)
            .build();

        when(repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationAttachmentId))
            .thenReturn(Mono.just(true));
        when(repository.findByTypeAndAttachmentIdAndRelationAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationAttachmentId))
            .thenReturn(Mono.just(existingEntity));

        StepVerifier.create(service.putAttachmentRelation(attachmentRelation))
            .assertNext(rel -> {
                assertThat(rel.getAttachmentId()).isEqualTo(attachmentId);
                assertThat(rel.getRelationAttachmentId()).isEqualTo(relationAttachmentId);
                assertThat(rel.getType()).isEqualTo(AttachmentRelationType.VIDEO_SUBTITLE);
            })
            .verifyComplete();
    }

    @Test
    void putAttachmentRelation_nullAttachmentRelation_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.putAttachmentRelation(null));
    }

    @Test
    void putAttachmentRelation_nullType_throwsException() {
        AttachmentRelation attachmentRelation = AttachmentRelation.builder()
            .attachmentId(UuidV7Utils.generateUuid())
            .type(null)
            .relationAttachmentId(UuidV7Utils.generateUuid())
            .build();

        assertThrows(IllegalArgumentException.class,
            () -> service.putAttachmentRelation(attachmentRelation));
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

        AttachmentRelationEntity entity1 = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(masterId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationId1)
            .build();
        AttachmentRelationEntity entity2 = AttachmentRelationEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(masterId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationId2)
            .build();

        when(repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, masterId, relationId1))
            .thenReturn(Mono.just(false));
        when(repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, masterId, relationId2))
            .thenReturn(Mono.just(false));
        doReturn(Mono.just(entity1)).doReturn(Mono.just(entity2)).when(repository)
            .insert(any(AttachmentRelationEntity.class));

        StepVerifier.create(service.putAttachmentRelations(param))
            .assertNext(rel -> assertThat(rel.getRelationAttachmentId()).isEqualTo(relationId1))
            .assertNext(rel -> assertThat(rel.getRelationAttachmentId()).isEqualTo(relationId2))
            .verifyComplete();
    }

    @Test
    void putAttachmentRelations_nullParam_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.putAttachmentRelations(null));
    }

    @Test
    void deleteAttachmentRelation_success() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationAttachmentId = UuidV7Utils.generateUuid();

        AttachmentRelation attachmentRelation = AttachmentRelation.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationAttachmentId)
            .build();

        when(repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationAttachmentId))
            .thenReturn(Mono.just(true));
        when(repository.deleteByTypeAndAttachmentIdAndRelationAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationAttachmentId))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.deleteAttachmentRelation(attachmentRelation))
            .assertNext(rel -> {
                assertThat(rel.getId()).isEqualTo(attachmentRelation.getId());
                assertThat(rel.getAttachmentId()).isEqualTo(attachmentId);
                assertThat(rel.getRelationAttachmentId()).isEqualTo(relationAttachmentId);
                assertThat(rel.getType()).isEqualTo(AttachmentRelationType.VIDEO_SUBTITLE);
            })
            .verifyComplete();
    }

    @Test
    void deleteAttachmentRelation_notFound_throwsException() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID relationAttachmentId = UuidV7Utils.generateUuid();

        AttachmentRelation attachmentRelation = AttachmentRelation.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(attachmentId)
            .type(AttachmentRelationType.VIDEO_SUBTITLE)
            .relationAttachmentId(relationAttachmentId)
            .build();

        when(repository.existsByTypeAndAttachmentIdAndRelationAttachmentId(
            AttachmentRelationType.VIDEO_SUBTITLE, attachmentId, relationAttachmentId))
            .thenReturn(Mono.just(false));

        StepVerifier.create(service.deleteAttachmentRelation(attachmentRelation))
            .verifyError(NotFoundException.class);
    }

    @Test
    void deleteAttachmentRelation_nullAttachmentRelation_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.deleteAttachmentRelation(null));
    }

    @Test
    void deleteAttachmentRelation_nullType_throwsException() {
        AttachmentRelation attachmentRelation = AttachmentRelation.builder()
            .id(UuidV7Utils.generateUuid())
            .attachmentId(UuidV7Utils.generateUuid())
            .type(null)
            .relationAttachmentId(UuidV7Utils.generateUuid())
            .build();

        assertThrows(IllegalArgumentException.class,
            () -> service.deleteAttachmentRelation(attachmentRelation));
    }
}
