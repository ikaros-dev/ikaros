package run.ikaros.server.core.subject;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectImage;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.SubjectType;

@SpringBootTest
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
            subjectService.findById(Long.MIN_VALUE).block();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
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
        AtomicReference<Long> subjectId = new AtomicReference<>();
        StepVerifier.create(subjectService.create(subject))
            .expectNextMatches(sub -> {
                subjectId.set(sub.getId());
                return Objects.nonNull(sub.getId());
            })
            .verifyComplete();

        assertThat(subjectId.get()).isGreaterThan(0);

        // Verify findById when subject record exists
        StepVerifier.create(subjectService.findById(subjectId.get()))
            .expectNextMatches(sub -> subject.canEqual(sub)
                && Objects.equals(subjectId.get(), sub.getId())
                && Objects.equals(subject.getName(), sub.getName())
                && subject.getType().equals(sub.getType())
                && Objects.nonNull(sub.getImage())
                && subject.getImage().getCommon()
                .equals(sub.getImage().getCommon()))
            .verifyComplete();
    }

    @Test
    void findByBgmIdWhenIdNotGtZero() {
        try {
            subjectService.findByBgmId(Long.MIN_VALUE, Long.MIN_VALUE).block();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void findByBgmIdWhenRecordNotExists() {
        StepVerifier.create(subjectService.findByBgmId(Long.MAX_VALUE, Long.MAX_VALUE))
            .expectErrorMessage("Not found subject by bgmtv_id: " + Long.MAX_VALUE)
            .verify();
    }

    @Test
    void findByBgmIdWhenSubjectExists() {
        var subject = createSubjectInstance();
        AtomicLong subjectId = new AtomicLong();
        StepVerifier.create(subjectService.create(subject))
            .expectNextMatches(subject1 -> {
                subjectId.set(subject1.getId());
                return Objects.nonNull(subject1.getId());
            })
            .verifyComplete();

        assertThat(subjectId.get()).isGreaterThan(0);

        // Verify findById when subject record exists
        StepVerifier.create(subjectService.findById(subjectId.get()))
            .expectNextMatches(sub -> subject.canEqual(sub)
                && Objects.equals(subjectId.get(), sub.getId())
                && Objects.equals(subject.getName(), sub.getName())
                && subject.getType().equals(sub.getType())
                && Objects.nonNull(sub.getImage())
                && subject.getImage().getCommon()
                .equals(sub.getImage().getCommon()))
            .verifyComplete();

        // Verify findByBgmId when subject record exists
        StepVerifier.create(subjectService.findByBgmId(subjectId.get(), Long.MAX_VALUE))
            .expectNextMatches(sub -> subject.canEqual(sub)
                && Objects.equals(subjectId.get(), sub.getId())
                && Objects.equals(subject.getName(), sub.getName())
                && subject.getType().equals(sub.getType())
                && Objects.nonNull(sub.getImage())
                && subject.getImage().getCommon()
                .equals(sub.getImage().getCommon()))
            .verifyComplete();
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

        var image = new SubjectImage();
        image.setCommon("https://ikaros.run/static/test.jpg");
        subject.setImage(image);

        var episodes = new ArrayList<Episode>();
        episodes.add(Episode.builder()
            .airTime(LocalDateTime.now())
            .name("ep-01")
            .nameCn("第一集").build());
        subject.setEpisodes(episodes)
            .setTotalEpisodes((long) episodes.size());

        var syncs = new ArrayList<SubjectSync>();
        syncs.add(SubjectSync.builder()
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId(String.valueOf(Long.MAX_VALUE))
            .syncTime(LocalDateTime.now())
            .build());
        subject.setSyncs(syncs);
        return subject;
    }

    @Test
    void deleteByIdWhenIdNotGtZero() {
        StepVerifier.create(subjectService.deleteById(Long.MIN_VALUE))
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
        Episode addEpisode = Episode.builder()
            .airTime(LocalDateTime.now())
            .name("ep-02")
            .nameCn("第二集").build();
        List<Episode> episodes = subject.getEpisodes();
        episodes.add(addEpisode);


        StepVerifier.create(subjectService.update(subject))
            .verifyComplete();

        StepVerifier.create(subjectService.findById(subject.getId()))
            .expectNextMatches(newSubject -> {
                    boolean result = true;
                    result = newName.equals(newSubject.getName());
                    result = newSubject.getTotalEpisodes() == 2;
                    if (!result) {
                        return false;
                    }

                    List<Episode> newEps = newSubject.getEpisodes();
                    Episode ep2 = newEps.get(1);
                    result = addEpisode.getName().equals(ep2.getName());
                    return result;
                }
            )
            .verifyComplete();


    }
}