package run.ikaros.server.core.subject;


import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.SubjectRelation;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.core.subject.service.SubjectRelationService;
import run.ikaros.server.store.repository.SubjectRelationRepository;

@Disabled
@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class SubjectRelationServiceTest {

    @Autowired
    SubjectRelationService subjectRelationService;
    @Autowired
    SubjectRelationRepository subjectRelationRepository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(subjectRelationRepository.deleteAll()).verifyComplete();
    }

    @Test
    void createSubjectRelation() {
        final UUID random = UuidV7Utils.generateUuid();
        SubjectRelation subjectRelation = SubjectRelation.builder()
            .subject(UuidV7Utils.generateUuid())
            .relationType(SubjectRelationType.COMIC)
            .relationSubjects(Set.of(random, UuidV7Utils.generateUuid()))
            .build();

        StepVerifier.create(subjectRelationService.findBySubjectIdAndType(
                UuidV7Utils.generateUuid(),
                SubjectRelationType.COMIC))
            .expectNextMatches(subjectRelation1 -> subjectRelation1.getRelationSubjects().isEmpty())
            .verifyComplete();

        StepVerifier.create(subjectRelationService.createSubjectRelation(subjectRelation))
            .expectNextMatches(subjectRelation1
                -> subjectRelation1.getRelationSubjects().contains(random))
            .verifyComplete();

        StepVerifier.create(
                subjectRelationService.findBySubjectIdAndType(UuidV7Utils.generateUuid(),
                    SubjectRelationType.COMIC))
            .expectNextMatches(subjectRelation1
                -> !subjectRelation1.getRelationSubjects().isEmpty())
            .verifyComplete();
    }

    @Test
    void removeSubjectRelation() {
        final UUID random = UuidV7Utils.generateUuid();
        SubjectRelation subjectRelation = SubjectRelation.builder()
            .subject(UuidV7Utils.generateUuid())
            .relationType(SubjectRelationType.COMIC)
            .relationSubjects(Set.of(random, UuidV7Utils.generateUuid()))
            .build();

        StepVerifier.create(
                subjectRelationService.findBySubjectIdAndType(UuidV7Utils.generateUuid(),
                    SubjectRelationType.COMIC))
            .expectNextMatches(subjectRelation1 -> subjectRelation1.getRelationSubjects().isEmpty())
            .verifyComplete();

        StepVerifier.create(subjectRelationService.createSubjectRelation(subjectRelation))
            .expectNextMatches(subjectRelation1
                -> subjectRelation1.getRelationSubjects().contains(random))
            .verifyComplete();

        SubjectRelation removeSubjectRelation = SubjectRelation.builder()
            .subject(UuidV7Utils.generateUuid())
            .relationType(SubjectRelationType.COMIC)
            .relationSubjects(Set.of(random))
            .build();
        StepVerifier.create(subjectRelationService.removeSubjectRelation(removeSubjectRelation))
            .expectNext(removeSubjectRelation).verifyComplete();

        StepVerifier.create(
                subjectRelationService.findBySubjectIdAndType(UuidV7Utils.generateUuid(),
                    SubjectRelationType.COMIC))
            .expectNextMatches(subjectRelation1
                -> !subjectRelation1.getRelationSubjects().contains(random)
                && subjectRelation1.getRelationSubjects().contains(9L))
            .verifyComplete();

    }

    @Test
    void findAllBySubjectId() {
        final UUID random = UuidV7Utils.generateUuid();
        SubjectRelation subjectRelation = SubjectRelation.builder()
            .subject(UuidV7Utils.generateUuid())
            .relationType(SubjectRelationType.COMIC)
            .relationSubjects(Set.of(random, UuidV7Utils.generateUuid()))
            .build();

        StepVerifier.create(subjectRelationService.findAllBySubjectId(UuidV7Utils.generateUuid()))
            .expectNextCount(0).verifyComplete();

        StepVerifier.create(
                subjectRelationService.findBySubjectIdAndType(UuidV7Utils.generateUuid(),
                    SubjectRelationType.COMIC))
            .expectNextMatches(subjectRelation1 -> subjectRelation1.getRelationSubjects().isEmpty())
            .verifyComplete();

        StepVerifier.create(subjectRelationService.createSubjectRelation(subjectRelation))
            .expectNextMatches(subjectRelation1
                -> subjectRelation1.getRelationSubjects().contains(random))
            .verifyComplete();

        StepVerifier.create(subjectRelationService.findAllBySubjectId(UuidV7Utils.generateUuid()))
            .expectNext(subjectRelation)
            .verifyComplete();

    }
}