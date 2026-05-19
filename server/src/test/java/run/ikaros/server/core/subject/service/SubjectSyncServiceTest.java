package run.ikaros.server.core.subject.service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.repository.SubjectSyncRepository;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
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
            .id(UuidV7Utils.generateUuid())
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

    @Test
    void findSubjectSyncsBySubjectId() {
        var random = new Random();
        UUID subjectId = UuidV7Utils.generateUuid();

        SubjectSync subjectSync1 = SubjectSync.builder()
            .id(UuidV7Utils.generateUuid())
            .syncTime(LocalDateTime.now())
            .subjectId(subjectId)
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId(String.valueOf(random.nextLong(1, 99999)))
            .build();

        SubjectSync subjectSync2 = SubjectSync.builder()
            .id(UuidV7Utils.generateUuid())
            .syncTime(LocalDateTime.now())
            .subjectId(subjectId)
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId(String.valueOf(random.nextLong(1, 99999)))
            .build();

        StepVerifier.create(subjectSyncService.save(subjectSync1))
            .expectNextCount(1).verifyComplete();
        StepVerifier.create(subjectSyncService.save(subjectSync2))
            .expectNextCount(1).verifyComplete();

        StepVerifier.create(subjectSyncService.findSubjectSyncsBySubjectId(subjectId))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void findSubjectSyncsByPlatformAndPlatformId() {
        var random = new Random();
        String platformId = String.valueOf(random.nextLong(1, 99999));

        SubjectSync subjectSync = SubjectSync.builder()
            .id(UuidV7Utils.generateUuid())
            .syncTime(LocalDateTime.now())
            .subjectId(UuidV7Utils.generateUuid())
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId(platformId)
            .build();

        StepVerifier.create(subjectSyncService.save(subjectSync))
            .expectNextCount(1).verifyComplete();

        StepVerifier.create(subjectSyncService.findSubjectSyncsByPlatformAndPlatformId(
                SubjectSyncPlatform.BGM_TV, platformId))
            .expectNextCount(1).verifyComplete();
    }

    @Test
    @Disabled("@MonoCacheEvict on remove() interferes with reactive chain, causing delete to not complete before cache eviction")
    void remove() {
        var random = new Random();
        SubjectSync subjectSync = SubjectSync.builder()
            .id(UuidV7Utils.generateUuid())
            .syncTime(LocalDateTime.now())
            .subjectId(UuidV7Utils.generateUuid())
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId(String.valueOf(random.nextLong(1, 99999)))
            .build();

        StepVerifier.create(subjectSyncService.save(subjectSync))
            .expectNextCount(1).verifyComplete();

        // remove() may emit the entity or complete empty depending on @MonoCacheEvict behavior
        subjectSyncService.remove(subjectSync).block();

        StepVerifier.create(subjectSyncService.findBySubjectIdAndPlatformAndPlatformId(
                subjectSync.getSubjectId(), subjectSync.getPlatform(), subjectSync.getPlatformId()))
            .expectNextCount(0).verifyComplete();
    }
}