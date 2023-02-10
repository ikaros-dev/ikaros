package run.ikaros.server.core.subject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest
class SubjectServiceTest {

    @Autowired
    SubjectService subjectService;


    @Test
    void findByIdWhenIdNotGtZero() {
        try {
            subjectService.findById(Long.MIN_VALUE).block();
        } catch (Exception e) {
            Assertions.assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void findByIdWhenRecordNotExists() {
        StepVerifier.create(subjectService.findById(Long.MAX_VALUE))
            .expectErrorMessage("Not found subject record by id: " + Long.MAX_VALUE)
            .verify();
    }


    @Test
    void findById() {
    }


    @Test
    void findByBgmIdWhenIdNotGtZero() {
        try {
            subjectService.findByBgmId(Long.MIN_VALUE).block();
        } catch (Exception e) {
            Assertions.assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void findByBgmIdWhenRecordNotExists() {
        StepVerifier.create(subjectService.findByBgmId(Long.MAX_VALUE))
            .expectErrorMessage("Not found subject by bgmtv_id: " + Long.MAX_VALUE)
            .verify();
    }

    @Test
    void findByBgmId() {
    }

    @Test
    void save() {
    }

    @Test
    void deleteById() {
    }
}