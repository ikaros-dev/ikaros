package run.ikaros.server.core.subject;


import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.SubjectRelation;
import run.ikaros.api.store.enums.SubjectRelationType;
import run.ikaros.server.core.subject.service.SubjectRelationService;
import run.ikaros.server.store.repository.SubjectRelationRepository;

@Disabled
@SpringBootTest
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
        final long random = new Random().nextLong(1, 100000);
        SubjectRelation subjectRelation = SubjectRelation.builder()
            .subject(Long.MAX_VALUE)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjects(Set.of(random, 9L))
            .build();

        StepVerifier.create(subjectRelationService.findBySubjectIdAndType(Long.MAX_VALUE,
                SubjectRelationType.COMIC))
            .expectNextMatches(subjectRelation1 -> subjectRelation1.getRelationSubjects().isEmpty())
            .verifyComplete();

        StepVerifier.create(subjectRelationService.createSubjectRelation(subjectRelation))
            .expectNextMatches(subjectRelation1
                -> subjectRelation1.getRelationSubjects().contains(random))
            .verifyComplete();

        StepVerifier.create(subjectRelationService.findBySubjectIdAndType(Long.MAX_VALUE,
                SubjectRelationType.COMIC))
            .expectNextMatches(subjectRelation1
                -> !subjectRelation1.getRelationSubjects().isEmpty())
            .verifyComplete();
    }

    @Test
    void removeSubjectRelation() {
        final long random = new Random().nextLong(1, 100000);
        SubjectRelation subjectRelation = SubjectRelation.builder()
            .subject(Long.MAX_VALUE)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjects(Set.of(random, 9L))
            .build();

        StepVerifier.create(subjectRelationService.findBySubjectIdAndType(Long.MAX_VALUE,
                SubjectRelationType.COMIC))
            .expectNextMatches(subjectRelation1 -> subjectRelation1.getRelationSubjects().isEmpty())
            .verifyComplete();

        StepVerifier.create(subjectRelationService.createSubjectRelation(subjectRelation))
            .expectNextMatches(subjectRelation1
                -> subjectRelation1.getRelationSubjects().contains(random))
            .verifyComplete();

        SubjectRelation removeSubjectRelation = SubjectRelation.builder()
            .subject(Long.MAX_VALUE)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjects(Set.of(random))
            .build();
        StepVerifier.create(subjectRelationService.removeSubjectRelation(removeSubjectRelation))
            .expectNext(removeSubjectRelation).verifyComplete();

        StepVerifier.create(subjectRelationService.findBySubjectIdAndType(Long.MAX_VALUE,
                SubjectRelationType.COMIC))
            .expectNextMatches(subjectRelation1
                -> !subjectRelation1.getRelationSubjects().contains(random)
                && subjectRelation1.getRelationSubjects().contains(9L))
            .verifyComplete();

    }

    @Test
    void findAllBySubjectId() {
        final long random = new Random().nextLong(1, 100000);
        SubjectRelation subjectRelation = SubjectRelation.builder()
            .subject(Long.MAX_VALUE)
            .relationType(SubjectRelationType.COMIC)
            .relationSubjects(Set.of(random, 9L))
            .build();

        StepVerifier.create(subjectRelationService.findAllBySubjectId(Long.MAX_VALUE))
            .expectNextCount(0).verifyComplete();

        StepVerifier.create(subjectRelationService.findBySubjectIdAndType(Long.MAX_VALUE,
                SubjectRelationType.COMIC))
            .expectNextMatches(subjectRelation1 -> subjectRelation1.getRelationSubjects().isEmpty())
            .verifyComplete();

        StepVerifier.create(subjectRelationService.createSubjectRelation(subjectRelation))
            .expectNextMatches(subjectRelation1
                -> subjectRelation1.getRelationSubjects().contains(random))
            .verifyComplete();

        StepVerifier.create(subjectRelationService.findAllBySubjectId(Long.MAX_VALUE))
            .expectNext(subjectRelation)
            .verifyComplete();

    }
}