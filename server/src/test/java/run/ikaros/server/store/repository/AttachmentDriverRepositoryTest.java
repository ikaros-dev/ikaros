package run.ikaros.server.store.repository;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.AttachmentDriverEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class AttachmentDriverRepositoryTest {

    @Autowired
    AttachmentDriverRepository repository;

    @Test
    void insert() {
        AttachmentDriverEntity entity = AttachmentDriverEntity
            .builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentDriverType.LOCAL)
            .enable(false)
            .avatar("")
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findById(entity.getId()))
            .expectNext(entity).verifyComplete();
    }
}