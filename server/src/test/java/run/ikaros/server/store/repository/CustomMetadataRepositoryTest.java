package run.ikaros.server.store.repository;


import java.nio.charset.StandardCharsets;
import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.store.entity.CustomMetadataEntity;

@SpringBootTest
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
            .customId(UuidV7Utils.generateUuid())
            .key(key)
            .value(oldValue.getBytes(StandardCharsets.UTF_8)).build();

        StepVerifier.create(repository.save(metadataEntity))
            .expectNext(metadataEntity).verifyComplete();

        // Update metadata entity value to new value.
        StepVerifier.create(repository
                .updateValueByCustomIdAndKeyAndValue(UuidV7Utils.generateUuid(), key,
                    newValue.getBytes(StandardCharsets.UTF_8)))
            .verifyComplete();

        // Verify.
        StepVerifier.create(repository.findByCustomIdAndKey(UuidV7Utils.generateUuid(), key)
                .map(CustomMetadataEntity::getValue)
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8)))
            .expectNext(newValue)
            .verifyComplete();
    }
}