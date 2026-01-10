package run.ikaros.server.store.repository;


import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class AttachmentReferenceRepositoryTest {

    @Autowired
    AttachmentReferenceRepository repository;


    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void findAllByType() {
        UUID attId1 = UuidV7Utils.generateUuid();
        UUID refId1 = UuidV7Utils.generateUuid();

        AttachmentReferenceEntity attRef1 = AttachmentReferenceEntity.builder()
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attId1)
            .referenceId(refId1)
            .build();
        StepVerifier.create(repository.save(attRef1))
            .expectNextMatches(entity -> attId1 == entity.getAttachmentId()
                && refId1 == entity.getReferenceId())
            .verifyComplete();

        StepVerifier.create(repository.findAllByTypeAndAttachmentId(
            AttachmentReferenceType.EPISODE, attId1
        )).expectNextMatches(entity -> entity.getAttachmentId() == attId1
            && entity.getReferenceId() == refId1).verifyComplete();

        StepVerifier.create(repository.findAllByTypeAndReferenceId(
            AttachmentReferenceType.EPISODE, refId1
        )).expectNextMatches(entity -> entity.getAttachmentId() == attId1
            && entity.getReferenceId() == refId1).verifyComplete();

    }
}