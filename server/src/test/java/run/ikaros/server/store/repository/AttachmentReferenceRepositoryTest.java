package run.ikaros.server.store.repository;


import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;

@SpringBootTest
class AttachmentReferenceRepositoryTest {

    @Autowired
    AttachmentReferenceRepository repository;


    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    Long randomLong() {
        return new Random().nextLong();
    }

    @Test
    void findAllByType() {
        long attId1 = randomLong();
        long refId1 = randomLong();

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