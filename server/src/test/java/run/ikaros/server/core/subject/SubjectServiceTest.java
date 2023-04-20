package run.ikaros.server.core.subject;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.store.enums.SubjectType;

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
    void findByIdWhenRecordExists() {
        var subject = createSubjectInstance();
        AtomicLong subjectId = new AtomicLong();
        try {
            StepVerifier.create(subjectService.save(subject))
                .expectNextMatches(subject1 -> {
                    boolean flag = Objects.nonNull(subject1.getId());
                    if (flag) {
                        subjectId.set(subject1.getId());
                    }
                    return flag;
                })
                .verifyComplete();

            // Verify findById when subject record exists
            StepVerifier.create(subjectService.findById(subjectId.get()))
                .expectNextMatches(subject1 -> subject.canEqual(subject1)
                    && subjectId.get() == subject1.getId()
                    && subject.getType().equals(subject1.getType())
                    && Objects.nonNull(subject1.getImage())
                    && subject.getImage().getCommon()
                    .equals(subject1.getImage().getCommon()))
                .verifyComplete();
        } finally {
            StepVerifier.create(subjectService.deleteById(subjectId.get()))
                .verifyComplete();
        }
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
    void findByBgmIdWhenSubjectExists() {
        var subject = createSubjectInstance();
        AtomicLong subjectId = new AtomicLong();
        try {
            StepVerifier.create(subjectService.save(subject))
                .expectNextMatches(subject1 -> {
                    boolean flag = Objects.nonNull(subject1.getId());
                    if (flag) {
                        subjectId.set(subject1.getId());
                    }
                    return flag;
                })
                .verifyComplete();

            // Verify findById when subject record exists
            StepVerifier.create(subjectService.findById(subjectId.get()))
                .expectNextMatches(subject1 -> subject.canEqual(subject1)
                    && subjectId.get() == subject1.getId()
                    && subject.getType().equals(subject1.getType())
                    && Objects.nonNull(subject1.getImage())
                    && subject.getImage().getCommon()
                    .equals(subject1.getImage().getCommon()))
                .verifyComplete();
        } finally {
            StepVerifier.create(subjectService.deleteById(subjectId.get()))
                .verifyComplete();
        }
    }

    @Test
    void saveAndDeleteById() {
        Subject subject = createSubjectInstance();

        AtomicLong subjectId = new AtomicLong();
        try {
            StepVerifier.create(subjectService.save(subject))
                .expectNextMatches(subject1 -> {
                    boolean flag = Objects.nonNull(subject1.getId());
                    if (flag) {
                        subjectId.set(subject1.getId());
                    }
                    return flag;
                })
                .verifyComplete();
        } finally {
            StepVerifier.create(subjectService.deleteById(subjectId.get()))
                .verifyComplete();
        }

    }

    private static Subject createSubjectInstance() {
        var subject = new Subject();
        subject.setName("subject-name-unit-test");
        subject.setType(SubjectType.ANIME);
        subject.setNsfw(Boolean.FALSE);
        subject.setInfobox("infobox-unit-test");
        subject.setNameCn("单元测试条目名");

        var image = new SubjectImage();
        image.setCommon("https://ikaros.run/static/test.jpg");
        subject.setImage(image);

        var episodes = new ArrayList<Episode>();
        episodes.add(Episode.builder()
            .subjectId(Long.MAX_VALUE)
            .airTime(LocalDateTime.now())
            .name("ep-01")
            .nameCn("第一集").build());
        subject.setEpisodes(episodes)
            .setTotalEpisodes((long) episodes.size());
        return subject;
    }

    @Test
    void deleteByIdWhenIdNotGtZero() {
        try {
            subjectService.deleteById(Long.MIN_VALUE).block();
        } catch (Exception e) {
            Assertions.assertThat(e).isInstanceOf(IllegalArgumentException.class);
            Assertions.assertThat(e.getMessage()).isEqualTo("'id' must gt 0.");
        }

    }
}