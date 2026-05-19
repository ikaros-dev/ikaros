package run.ikaros.server.store.repository;


import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.CustomMetadataEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class CustomMetadataRepositoryTest {
    @Autowired
    CustomMetadataRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void updateByCustomIdAndKey() {
        // Create a metadata entity record.
        final String key = Long.valueOf(new Random().nextLong()).toString();
        final String oldValue = Long.valueOf(new Random().nextLong()).toString();
        final String newValue = Long.valueOf(new Random().nextLong()).toString();
        CustomMetadataEntity metadataEntity = CustomMetadataEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .customId(UuidV7Utils.generateUuid())
            .key(key)
            .value(oldValue.getBytes(StandardCharsets.UTF_8)).build();

        StepVerifier.create(repository.insert(metadataEntity))
            .expectNext(metadataEntity).verifyComplete();

        // Update metadata entity value to new value.
        StepVerifier.create(repository
                .updateValueByCustomIdAndKeyAndValue(metadataEntity.getCustomId(), key,
                    newValue.getBytes(StandardCharsets.UTF_8)))
            .verifyComplete();

        // Verify.
        StepVerifier.create(repository.findByCustomIdAndKey(metadataEntity.getCustomId(), key)
                .map(CustomMetadataEntity::getValue)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8)))
            .expectNext(newValue)
            .verifyComplete();
    }

    @Test
    void findByCustomIdAndKey() {
        final String key = "test-key-" + UuidV7Utils.generate().substring(0, 8);
        final String value = "test-value-" + UuidV7Utils.generate().substring(0, 8);

        CustomMetadataEntity metadataEntity = CustomMetadataEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .customId(UuidV7Utils.generateUuid())
            .key(key)
            .value(value.getBytes(StandardCharsets.UTF_8))
            .build();

        StepVerifier.create(repository.insert(metadataEntity))
            .expectNext(metadataEntity).verifyComplete();

        StepVerifier.create(repository.findByCustomIdAndKey(metadataEntity.getCustomId(), key))
            .expectNextMatches(entity ->
                entity.getCustomId().equals(metadataEntity.getCustomId())
                    && entity.getKey().equals(key))
            .verifyComplete();
    }

    @Test
    void findAllByCustomId() {
        UUID customId = UuidV7Utils.generateUuid();

        CustomMetadataEntity entity1 = CustomMetadataEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .customId(customId)
            .key("key1")
            .value("value1".getBytes(StandardCharsets.UTF_8))
            .build();

        CustomMetadataEntity entity2 = CustomMetadataEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .customId(customId)
            .key("key2")
            .value("value2".getBytes(StandardCharsets.UTF_8))
            .build();

        StepVerifier.create(repository.insert(entity1))
            .expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2))
            .expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllByCustomId(customId))
            .expectNextCount(2).verifyComplete();
    }

    @Test
    void deleteAllByCustomId() {
        UUID customId = UuidV7Utils.generateUuid();

        CustomMetadataEntity entity1 = CustomMetadataEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .customId(customId)
            .key("key1")
            .value("value1".getBytes(StandardCharsets.UTF_8))
            .build();

        CustomMetadataEntity entity2 = CustomMetadataEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .customId(customId)
            .key("key2")
            .value("value2".getBytes(StandardCharsets.UTF_8))
            .build();

        StepVerifier.create(repository.insert(entity1))
            .expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2))
            .expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.deleteAllByCustomId(customId))
            .verifyComplete();

        StepVerifier.create(repository.findAllByCustomId(customId))
            .expectNextCount(0).verifyComplete();
    }
}