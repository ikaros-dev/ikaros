package run.ikaros.server.core.subject;

import static org.assertj.core.api.Assertions.assertThat;
import static run.ikaros.api.constant.AppConst.BLOCK_TIMEOUT;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.core.subject.service.SubjectService;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class SubjectServiceTest {

    @Autowired
    SubjectService subjectService;


    @AfterEach
    void tearDown() {
        StepVerifier.create(subjectService.deleteAll()).verifyComplete();
    }

    @Test
    void findByIdWhenIdNotGtZero() {
        try {
            subjectService.findById(UuidV7Utils.generateUuid()).block(AppConst.BLOCK_TIMEOUT);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void findByIdWhenRecordNotExists() {
        StepVerifier.create(subjectService.findById(UuidV7Utils.generateUuid()))
            .verifyComplete();
    }

    @Test
    void findByIdWhenRecordExists() {
        var subject = createSubjectInstance();
        AtomicReference<UUID> subjectId = new AtomicReference<>();
        StepVerifier.create(subjectService.create(subject))
            .expectNextMatches(sub -> {
                subjectId.set(sub.getId());
                return Objects.nonNull(sub.getId());
            })
            .verifyComplete();

        assertThat(subjectId.get()).isNotNull();

        // Verify findById when subject record exists
        StepVerifier.create(subjectService.findById(subjectId.get()))
            .expectNextMatches(sub -> Objects.equals(subjectId.get(), sub.getId())
                && Objects.equals(subject.getName(), sub.getName())
                && subject.getType().equals(sub.getType()))
            .verifyComplete();
    }

    @Test
    void findByBgmIdWhenIdNotGtZero() {
        try {
            subjectService.findByBgmId(UuidV7Utils.generateUuid(), UuidV7Utils.generate())
                .block(BLOCK_TIMEOUT);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void findByBgmIdWhenRecordNotExists() {
        StepVerifier.create(
                subjectService.findByBgmId(UuidV7Utils.generateUuid(), UuidV7Utils.generate()))
            .expectErrorMessage("Not found subject by bgmtv_id: " + UuidV7Utils.generateUuid())
            .verify();
    }

    @Test
    void findByBgmIdWhenSubjectExists() {
        var subject = createSubjectInstance();
        AtomicReference<UUID> subjectId = new AtomicReference<>(UuidV7Utils.generateUuid());
        StepVerifier.create(subjectService.create(subject))
            .expectNextMatches(subject1 -> {
                subjectId.set(subject1.getId());
                return Objects.nonNull(subject1.getId());
            })
            .verifyComplete();

        assertThat(subjectId.get()).isNotNull();

        // Verify findById when subject record exists
        StepVerifier.create(subjectService.findById(subjectId.get()))
            .expectNextMatches(sub -> Objects.equals(subjectId.get(), sub.getId())
                && Objects.equals(subject.getName(), sub.getName())
                && subject.getType().equals(sub.getType())
            )
            .verifyComplete();

        // Verify findByBgmId when subject record exists
        StepVerifier.create(subjectService.findByBgmId(subjectId.get(), UuidV7Utils.generate()))
            .expectError(NotFoundException.class)
            .verify();
    }

    @Test
    void create() {
        Subject subject = createSubjectInstance();
        StepVerifier.create(subjectService.create(subject))
            .expectNextMatches(sub -> Objects.nonNull(sub.getId()))
            .verifyComplete();
    }

    private static Subject createSubjectInstance() {
        var subject = new Subject();
        subject.setName("subject-name-unit-test");
        subject.setType(SubjectType.ANIME);
        subject.setNsfw(Boolean.FALSE);
        subject.setInfobox("infobox-unit-test");
        subject.setNameCn("单元测试条目名");
        subject.setAirTime(LocalDateTime.now());
        subject.setCover("https://ikaros.run/static/test.jpg");
        return subject;
    }

    @Test
    void deleteByIdWhenIdNotGtZero() {
        StepVerifier.create(subjectService.deleteById(UuidV7Utils.generateUuid()))
            .expectErrorMatches(throwable -> (throwable instanceof IllegalArgumentException)
                && "'id' must gt 0.".equals(throwable.getMessage())).verify();
    }

    @Test
    void update() {
        Subject subject = createSubjectInstance();

        StepVerifier.create(subjectService.create(subject))
            .expectNext(subject)
            .verifyComplete();

        assertThat(subject.getId()).isNotNull();

        // update
        final String newName = "subject-name-unit-test";
        subject.setName(newName);


        StepVerifier.create(subjectService.update(subject))
            .verifyComplete();


    }

    @Test
    void updateLarge() {
        Subject subject = createSubjectInstance();
        final Random random = new Random();

        StepVerifier.create(subjectService.create(subject))
            .expectNext(subject)
            .verifyComplete();

        assertThat(subject.getId()).isNotNull();

        // update
        final String newName = "subject-name-unit-test";
        subject.setName(newName);


        final int epCount = 100;


        StepVerifier.create(subjectService.update(subject))
            .verifyComplete();

        StepVerifier.create(subjectService.findById(subject.getId()))
            .expectNextMatches(newSubject -> {
                    boolean result = true;
                    result = newName.equals(newSubject.getName());
                    if (!result) {
                        return false;
                    }
                    return result;
                }
            )
            .verifyComplete();


    }
}