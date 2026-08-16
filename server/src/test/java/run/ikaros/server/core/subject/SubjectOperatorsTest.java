package run.ikaros.server.core.subject;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectRelation;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.service.SubjectRelationService;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.subject.service.SubjectSyncService;

@ExtendWith(MockitoExtension.class)
@org.jspecify.annotations.NullUnmarked
class SubjectOperatorsTest {

    private SubjectService subjectService;
    private SubjectSyncService syncService;
    private SubjectRelationService relationService;

    private SubjectOperator subjectOperator;
    private SubjectRelationOperator relationOperator;
    private SubjectSyncOperator syncOperator;

    @BeforeEach
    void setUp() {
        subjectService = mock(SubjectService.class);
        syncService = mock(SubjectSyncService.class);
        relationService = mock(SubjectRelationService.class);
        subjectOperator = new SubjectOperator(subjectService, syncService);
        relationOperator = new SubjectRelationOperator(relationService);
        syncOperator = new SubjectSyncOperator(syncService);
    }

    @Test
    void findById() {
        UUID id = UuidV7Utils.generateUuid();
        Subject subject = Subject
            .builder()
            .id(id)
            .name("Test")
            .build();
        when(subjectService.findById(id)).thenReturn(Mono.just(subject));
        StepVerifier
            .create(subjectOperator.findById(id))
            .expectNext(subject)
            .verifyComplete();
    }

    @Test
    void create() {
        Subject subject = Subject
            .builder()
            .name("New")
            .build();
        when(subjectService.create(any())).thenReturn(Mono.just(subject));
        StepVerifier
            .create(subjectOperator.create(subject))
            .expectNext(subject)
            .verifyComplete();
    }

    @Test
    void update() {
        Subject subject = Subject
            .builder()
            .id(UuidV7Utils.generateUuid())
            .name("Updated")
            .build();
        when(subjectService.update(any())).thenReturn(Mono.empty());
        StepVerifier
            .create(subjectOperator.update(subject))
            .verifyComplete();
    }

    @Test
    void findBySubjectIdAndPlatformAndPlatformId() {
        UUID subjectId = UuidV7Utils.generateUuid();
        Subject subject = Subject
            .builder()
            .name("SyncTest")
            .build();
        when(subjectService.findBySubjectIdAndPlatformAndPlatformId(subjectId,
            SubjectSyncPlatform.BGM_TV, "bgm123"))
            .thenReturn(Mono.just(subject));
        StepVerifier
            .create(
                subjectOperator.findBySubjectIdAndPlatformAndPlatformId(subjectId,
                    SubjectSyncPlatform.BGM_TV, "bgm123"))
            .expectNext(subject)
            .verifyComplete();
    }

    @Test
    void relationFindAllBySubjectId() {
        UUID subjectId = UuidV7Utils.generateUuid();
        SubjectRelation relation = SubjectRelation
            .builder()
            .subject(subjectId)
            .relationType(SubjectRelationType.AFTER)
            .relationSubjects(Set.of(UuidV7Utils.generateUuid()))
            .build();
        when(relationService.findAllBySubjectId(subjectId)).thenReturn(Flux.just(relation));
        StepVerifier
            .create(relationOperator.findAllBySubjectId(subjectId))
            .expectNext(relation)
            .verifyComplete();
    }

    @Test
    void relationFindBySubjectIdAndType() {
        UUID subjectId = UuidV7Utils.generateUuid();
        SubjectRelation relation = SubjectRelation
            .builder()
            .subject(subjectId)
            .relationType(SubjectRelationType.AFTER)
            .build();
        when(relationService.findBySubjectIdAndType(subjectId, SubjectRelationType.AFTER))
            .thenReturn(Mono.just(relation));
        StepVerifier
            .create(relationOperator.findBySubjectIdAndType(subjectId, SubjectRelationType.AFTER))
            .expectNext(relation)
            .verifyComplete();
    }

    @Test
    void relationCreateSubjectRelation() {
        SubjectRelation relation = SubjectRelation
            .builder()
            .relationType(SubjectRelationType.AFTER)
            .build();
        when(relationService.createSubjectRelation(relation)).thenReturn(Mono.just(relation));
        StepVerifier
            .create(relationOperator.createSubjectRelation(relation))
            .expectNext(relation)
            .verifyComplete();
    }

    @Test
    void sync() {
        UUID subjectId = UuidV7Utils.generateUuid();
        when(syncService.sync(subjectId, SubjectSyncPlatform.BGM_TV, "456"))
            .thenReturn(Mono.empty());
        StepVerifier
            .create(syncOperator.sync(subjectId, SubjectSyncPlatform.BGM_TV, "456"))
            .verifyComplete();
    }

    @Test
    void syncWithoutSubjectId() {
        when(syncService.sync(null, SubjectSyncPlatform.BGM_TV, "456"))
            .thenReturn(Mono.empty());
        StepVerifier
            .create(syncOperator.sync(null, SubjectSyncPlatform.BGM_TV, "456"))
            .verifyComplete();
    }

    @Test
    void saveSubjectSync() {
        SubjectSync sync = SubjectSync
            .builder()
            .platformId("bgm123")
            .build();
        when(syncService.save(sync)).thenReturn(Mono.just(sync));
        StepVerifier
            .create(syncOperator.save(sync))
            .expectNext(sync)
            .verifyComplete();
    }

    @Test
    void findSubjectSyncsBySubjectId() {
        UUID subjectId = UuidV7Utils.generateUuid();
        SubjectSync sync = SubjectSync
            .builder()
            .subjectId(subjectId)
            .build();
        when(syncService.findSubjectSyncsBySubjectId(subjectId)).thenReturn(Flux.just(sync));
        StepVerifier
            .create(syncOperator.findSubjectSyncsBySubjectId(subjectId))
            .expectNext(sync)
            .verifyComplete();
    }

    @Test
    void findSubjectSyncBySubjectIdAndPlatform() {
        UUID subjectId = UuidV7Utils.generateUuid();
        SubjectSync sync = SubjectSync
            .builder()
            .subjectId(subjectId)
            .build();
        when(syncService.findSubjectSyncBySubjectIdAndPlatform(subjectId,
            SubjectSyncPlatform.BGM_TV))
            .thenReturn(Mono.just(sync));
        StepVerifier
            .create(syncOperator.findSubjectSyncBySubjectIdAndPlatform(subjectId,
                SubjectSyncPlatform.BGM_TV))
            .expectNext(sync)
            .verifyComplete();
    }
}
