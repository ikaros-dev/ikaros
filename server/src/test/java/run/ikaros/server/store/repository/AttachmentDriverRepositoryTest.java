package run.ikaros.server.store.repository;


import org.junit.jupiter.api.AfterEach;
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

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

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

    @Test
    void findByTypeAndName() {
        String name = "test-driver-" + UuidV7Utils.generate().substring(0, 8);

        AttachmentDriverEntity entity = AttachmentDriverEntity
            .builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentDriverType.LOCAL)
            .name(name)
            .enable(false)
            .avatar("")
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findByTypeAndName("LOCAL", name))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findByTypeAndNameAndMountName() {
        String name = "test-driver-" + UuidV7Utils.generate().substring(0, 8);
        String mountName = "mount-" + UuidV7Utils.generate().substring(0, 8);

        AttachmentDriverEntity entity = AttachmentDriverEntity
            .builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentDriverType.LOCAL)
            .name(name)
            .mountName(mountName)
            .enable(false)
            .avatar("")
            .build();

        StepVerifier.create(repository.insert(entity))
            .expectNext(entity).verifyComplete();

        StepVerifier.create(repository.findByTypeAndNameAndMountName("LOCAL", name, mountName))
            .expectNext(entity).verifyComplete();
    }

    @Test
    void findAllByTypeAndEnable() {
        String name1 = "test-driver-1-" + UuidV7Utils.generate().substring(0, 8);
        String name2 = "test-driver-2-" + UuidV7Utils.generate().substring(0, 8);

        AttachmentDriverEntity entity1 = AttachmentDriverEntity
            .builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentDriverType.LOCAL)
            .name(name1)
            .enable(true)
            .avatar("")
            .build();

        AttachmentDriverEntity entity2 = AttachmentDriverEntity
            .builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentDriverType.LOCAL)
            .name(name2)
            .enable(false)
            .avatar("")
            .build();

        StepVerifier.create(repository.insert(entity1))
            .expectNext(entity1).verifyComplete();
        StepVerifier.create(repository.insert(entity2))
            .expectNext(entity2).verifyComplete();

        StepVerifier.create(repository.findAllByTypeAndEnable("LOCAL", true))
            .expectNext(entity1).verifyComplete();

        StepVerifier.create(repository.findAllByTypeAndEnable("LOCAL", false))
            .expectNext(entity2).verifyComplete();
    }
}