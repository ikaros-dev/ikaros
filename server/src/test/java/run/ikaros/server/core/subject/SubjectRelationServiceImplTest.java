package run.ikaros.server.core.subject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.SubjectRelation;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.server.core.subject.service.impl.SubjectRelationServiceImpl;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.entity.SubjectRelationEntity;
import run.ikaros.server.store.repository.SubjectRelationRepository;
import run.ikaros.server.store.repository.SubjectRepository;

class SubjectRelationServiceImplTest {

    @Mock
    private SubjectRelationRepository subjectRelationRepository;
    @Mock
    private SubjectRepository subjectRepository;
    private SubjectRelationServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new SubjectRelationServiceImpl(subjectRelationRepository, subjectRepository);
    }

    @Test
    void findAllBySubjectId_groupsByType() {
        UUID subjectId = UuidV7Utils.generateUuid();
        UUID relId1 = UuidV7Utils.generateUuid();
        UUID relId2 = UuidV7Utils.generateUuid();
        UUID relId3 = UuidV7Utils.generateUuid();

        SubjectRelationEntity entity1 = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.ANIME)
            .relationSubjectId(relId1)
            .build();
        entity1.setId(UuidV7Utils.generateUuid());

        SubjectRelationEntity entity2 = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.ANIME)
            .relationSubjectId(relId2)
            .build();
        entity2.setId(UuidV7Utils.generateUuid());

        SubjectRelationEntity entity3 = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjectId(relId3)
            .build();
        entity3.setId(UuidV7Utils.generateUuid());

        when(subjectRelationRepository.findAllBySubjectId(subjectId))
            .thenReturn(Flux.just(entity1, entity2, entity3));

        StepVerifier.create(service.findAllBySubjectId(subjectId).collectList())
            .assertNext(list -> {
                assertThat(list).hasSize(2);
                assertThat(list).anySatisfy(rel -> {
                    assertThat(rel.getSubject()).isEqualTo(subjectId);
                    assertThat(rel.getRelationType()).isEqualTo(SubjectRelationType.ANIME);
                    assertThat(rel.getRelationSubjects()).containsExactlyInAnyOrder(relId1, relId2);
                });
                assertThat(list).anySatisfy(rel -> {
                    assertThat(rel.getSubject()).isEqualTo(subjectId);
                    assertThat(rel.getRelationType()).isEqualTo(SubjectRelationType.COMIC);
                    assertThat(rel.getRelationSubjects()).containsExactly(relId3);
                });
            })
            .verifyComplete();
    }

    @Test
    void findAllBySubjectId_emptyResult() {
        UUID subjectId = UuidV7Utils.generateUuid();
        when(subjectRelationRepository.findAllBySubjectId(subjectId))
            .thenReturn(Flux.empty());

        StepVerifier.create(service.findAllBySubjectId(subjectId))
            .verifyComplete();
    }

    @Test
    void findBySubjectIdAndType() {
        UUID subjectId = UuidV7Utils.generateUuid();
        UUID relId1 = UuidV7Utils.generateUuid();
        UUID relId2 = UuidV7Utils.generateUuid();

        SubjectRelationEntity entity1 = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.ANIME)
            .relationSubjectId(relId1)
            .build();
        entity1.setId(UuidV7Utils.generateUuid());

        SubjectRelationEntity entity2 = SubjectRelationEntity.builder()
            .subjectId(subjectId)
            .relationType(SubjectRelationType.ANIME)
            .relationSubjectId(relId2)
            .build();
        entity2.setId(UuidV7Utils.generateUuid());

        when(subjectRelationRepository.findAllBySubjectIdAndRelationType(
            subjectId, SubjectRelationType.ANIME))
            .thenReturn(Flux.just(entity1, entity2));

        StepVerifier.create(service.findBySubjectIdAndType(subjectId, SubjectRelationType.ANIME))
            .assertNext(rel -> {
                assertThat(rel.getSubject()).isEqualTo(subjectId);
                assertThat(rel.getRelationType()).isEqualTo(SubjectRelationType.ANIME);
                assertThat(rel.getRelationSubjects()).containsExactlyInAnyOrder(relId1, relId2);
            })
            .verifyComplete();
    }

    @Test
    void findBySubjectIdAndType_nullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.findBySubjectIdAndType(UuidV7Utils.generateUuid(), null));
    }

    @Test
    void saveEntity_insertsWhenIdIsNull() {
        SubjectRelationEntity entity = SubjectRelationEntity.builder()
            .subjectId(UuidV7Utils.generateUuid())
            .relationType(SubjectRelationType.ANIME)
            .relationSubjectId(UuidV7Utils.generateUuid())
            .build();

        SubjectRelationEntity savedEntity = SubjectRelationEntity.builder()
            .subjectId(entity.getSubjectId())
            .relationType(entity.getRelationType())
            .relationSubjectId(entity.getRelationSubjectId())
            .build();
        savedEntity.setId(UuidV7Utils.generateUuid());

        when(subjectRelationRepository.insert(any(SubjectRelationEntity.class)))
            .thenReturn(Mono.just(savedEntity));

        StepVerifier.create(service.saveEntity(entity))
            .assertNext(result -> assertThat(result.getId()).isNotNull())
            .verifyComplete();
    }

    @Test
    void saveEntity_updatesWhenIdExists() {
        UUID entityId = UuidV7Utils.generateUuid();
        SubjectRelationEntity entity = SubjectRelationEntity.builder()
            .subjectId(UuidV7Utils.generateUuid())
            .relationType(SubjectRelationType.ANIME)
            .relationSubjectId(UuidV7Utils.generateUuid())
            .build();
        entity.setId(entityId);

        when(subjectRelationRepository.update(any(SubjectRelationEntity.class)))
            .thenReturn(Mono.just(entity));

        StepVerifier.create(service.saveEntity(entity))
            .assertNext(result -> assertThat(result.getId()).isEqualTo(entityId))
            .verifyComplete();
    }

    @Test
    void createSubjectRelation_nullSubjectRelation_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.createSubjectRelation(null));
    }

    @Test
    void removeSubjectRelation_nullSubjectRelation_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.removeSubjectRelation(null));
    }
}
