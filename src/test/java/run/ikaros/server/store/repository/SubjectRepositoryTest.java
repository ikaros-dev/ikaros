package run.ikaros.server.store.repository;

import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.enums.SubjectType;

@SpringBootTest
class SubjectRepositoryTest {

    @Autowired
    SubjectRepository subjectRepository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(subjectRepository.deleteAll()).verifyComplete();
    }

    @Test
    void findById() {
        final String name = "test" + new Random(100).nextInt();
        SubjectEntity subject = SubjectEntity.builder()
            .name(name)
            .type(SubjectType.ANIME.getCode())
            .nsfw(false)
            .build();
        StepVerifier.create(subjectRepository.save(subject))
            .expectNextMatches(subjectEntity -> {
                subject.setId(subjectEntity.getId());
                return name.equals(subjectEntity.getName());
            })
            .verifyComplete();


        StepVerifier.create(subjectRepository.findById(subject.getId()))
            .expectNextMatches(subjectEntity -> name.equalsIgnoreCase(subjectEntity.getName()))
            .verifyComplete();
    }
}