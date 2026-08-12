package run.ikaros.server.store.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.EpisodeListEpisodeEntity;

/** 歌单歌曲关系仓储的 PostgreSQL 集成测试. */
@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class EpisodeListEpisodeRepositoryTest {

    /** 待测试的歌单歌曲关系仓储. */
    @Autowired
    EpisodeListEpisodeRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void shouldPerformBasicCrud() {
        UUID episodeListId = UuidV7Utils.generateUuid();
        UUID initialEpisodeId = UuidV7Utils.generateUuid();
        EpisodeListEpisodeEntity entity = relation(episodeListId, initialEpisodeId);

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity)
            .verifyComplete();
        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity)
            .verifyComplete();

        UUID updatedEpisodeId = UuidV7Utils.generateUuid();
        entity.setEpisodeId(updatedEpisodeId);
        StepVerifier.create(repository.update(entity))
            .assertNext(updated -> assertThat(updated.getEpisodeId()).isEqualTo(updatedEpisodeId))
            .verifyComplete();
        StepVerifier.create(repository.findAllByEpisodeListId(episodeListId).single())
            .assertNext(found -> assertThat(found.getEpisodeId()).isEqualTo(updatedEpisodeId))
            .verifyComplete();

        StepVerifier.create(repository.deleteById(entity.getId())).verifyComplete();
        StepVerifier.create(repository.findById(entity.getId())).verifyComplete();
    }

    @Test
    void insertAllShouldSkipEmptyList() {
        UUID episodeListId = UuidV7Utils.generateUuid();

        StepVerifier.create(repository.insertAll(episodeListId, List.of())).verifyComplete();

        StepVerifier.create(repository.findAllByEpisodeListId(episodeListId)).verifyComplete();
    }

    @Test
    void insertAllShouldInsertOneRelationship() {
        verifyBatchInsert(1);
    }

    @Test
    void insertAllShouldInsertOneThousandRelationships() {
        verifyBatchInsert(1000);
    }

    @Test
    void insertAllShouldInsertOneThousandAndOneRelationships() {
        verifyBatchInsert(1001);
    }

    private void verifyBatchInsert(int count) {
        UUID episodeListId = UuidV7Utils.generateUuid();
        List<UUID> episodeIds = IntStream.range(0, count)
            .mapToObj(index -> UuidV7Utils.generateUuid())
            .toList();

        StepVerifier.create(repository.insertAll(episodeListId, episodeIds)).verifyComplete();

        StepVerifier.create(repository.findAllByEpisodeListId(episodeListId).collectList())
            .assertNext(relations -> {
                assertThat(relations).hasSize(count);
                Set<UUID> actualEpisodeIds = relations.stream()
                    .map(EpisodeListEpisodeEntity::getEpisodeId)
                    .collect(java.util.stream.Collectors.toSet());
                assertThat(actualEpisodeIds).containsExactlyInAnyOrderElementsOf(episodeIds);
                assertThat(actualEpisodeIds).hasSize(count);
            })
            .verifyComplete();
    }

    private EpisodeListEpisodeEntity relation(UUID episodeListId, UUID episodeId) {
        return EpisodeListEpisodeEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .episodeListId(episodeListId)
            .episodeId(episodeId)
            .build();
    }
}
