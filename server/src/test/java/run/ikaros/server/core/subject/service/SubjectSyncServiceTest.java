package run.ikaros.server.core.subject.service;

import java.time.LocalDateTime;
import java.util.Random;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.store.repository.SubjectSyncRepository;

@SpringBootTest
class SubjectSyncServiceTest {

    @Autowired
    private SubjectSyncService subjectSyncService;
    @Autowired
    private SubjectSyncRepository subjectSyncRepository;

    @BeforeEach
    void setUp() {
        StepVerifier.create(subjectSyncRepository.deleteAll()).verifyComplete();
    }

    @Test
    void save() {
        var random = new Random();
        SubjectSync subjectSync = SubjectSync.builder()
            .syncTime(LocalDateTime.now())
            .subjectId(UuidV7Utils.generateUuid())
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId(String.valueOf(random.nextLong(1, 99999)))
            .build();

        StepVerifier.create(subjectSyncService.save(subjectSync))
            .expectNextMatches(subjectSync1 ->
                subjectSync.getSubjectId().equals(subjectSync1.getSubjectId())
                    && subjectSync.getPlatformId().equals(subjectSync1.getPlatformId())
                    && subjectSync.getSyncTime().equals(subjectSync1.getSyncTime())
                    && subjectSync.getPlatform().equals(subjectSync1.getPlatform()))
            .verifyComplete();


        StepVerifier.create(subjectSyncService.findBySubjectIdAndPlatformAndPlatformId(
                subjectSync.getSubjectId(), subjectSync.getPlatform(), subjectSync.getPlatformId()
            )).expectNextMatches(subjectSync1 ->
                subjectSync.getSubjectId().equals(subjectSync1.getSubjectId())
                    && subjectSync.getPlatformId().equals(subjectSync1.getPlatformId())
                    && subjectSync.getPlatform().name().equals(subjectSync1.getPlatform().name())
            )
            .verifyComplete();
    }
}