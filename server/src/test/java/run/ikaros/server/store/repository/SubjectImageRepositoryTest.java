package run.ikaros.server.store.repository;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.server.store.entity.SubjectImageEntity;

@SpringBootTest
class SubjectImageRepositoryTest {

    @Autowired
    SubjectImageRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void findBySubjectId() {
        SubjectImageEntity subjectImageEntity = SubjectImageEntity.builder()
            .subjectId(Long.MAX_VALUE)
            .common("test")
            .build();
        StepVerifier.create(repository.save(subjectImageEntity))
            .expectNextMatches(
                subjectImageEntity1 -> Long.MAX_VALUE == subjectImageEntity1.getSubjectId()
                    && "test".equalsIgnoreCase(subjectImageEntity1.getCommon()))
            .verifyComplete();

        StepVerifier.create(repository.findBySubjectId(Long.MAX_VALUE))
            .expectNextMatches(
                subjectImageEntity1 -> Long.MAX_VALUE == subjectImageEntity1.getSubjectId()
                    && "test".equalsIgnoreCase(subjectImageEntity1.getCommon()))
            .verifyComplete();
    }
}