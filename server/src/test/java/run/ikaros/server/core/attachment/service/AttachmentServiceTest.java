package run.ikaros.server.core.attachment.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.Predicate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.FileConst;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.infra.utils.RandomUtils;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

@SpringBootTest
class AttachmentServiceTest {
    @Autowired
    AttachmentService attachmentService;
    @Autowired
    AttachmentRepository attachmentRepository;
    @Autowired
    IkarosProperties ikarosProperties;

    @AfterEach
    void tearDown() throws IOException {
        Path uploadFileBasePath = ikarosProperties.getWorkDir().resolve(FileConst.DEFAULT_DIR_NAME);
        FileUtils.deleteDirByRecursion(uploadFileBasePath.toString());
        StepVerifier.create(attachmentRepository.deleteAll()).verifyComplete();
    }

    @BeforeEach
    void setUp() {
        StepVerifier.create(attachmentRepository.deleteAll()).verifyComplete();
    }

    @Test
    @Disabled
    void upload() throws IOException {
        StepVerifier.create(attachmentService.listEntitiesByCondition(
                    AttachmentSearchCondition.builder().build())
                .map(PagingWrap::getTotal))
            .expectNext(3L)
            .verifyComplete();

        final String name = "UnitTestDocFile.TXT";
        ClassPathResource classPathResource =
            new ClassPathResource("core/file/" + name);

        Flux<DataBuffer> dataBufferFlux =
            FileUtils.convertToDataBufferFlux(classPathResource.getFile());

        StepVerifier.create(attachmentService.upload(
                AttachmentUploadCondition.builder()
                    .dataBufferFlux(dataBufferFlux)
                    .name(name)
                    .build()))
            .expectNextMatches(attachment -> StringUtils.hasText(attachment.getFsPath()))
            .verifyComplete();

        StepVerifier.create(attachmentService.findByTypeAndParentIdAndName(AttachmentType.File,
                null, name).map(Attachment::getName))
            .expectNext(name)
            .verifyComplete();

        // remove attachment
        StepVerifier.create(attachmentService.removeByTypeAndParentIdAndName(AttachmentType.File,
            null, name)).verifyComplete();

        StepVerifier.create(attachmentService.findByTypeAndParentIdAndName(AttachmentType.File,
                null, name).map(Attachment::getName))
            .verifyComplete();

    }

    static class AttachmentEntityPredicate implements Predicate<AttachmentEntity> {
        private final AttachmentEntity oldEntity;

        AttachmentEntityPredicate(AttachmentEntity oldEntity) {
            this.oldEntity = oldEntity;
        }

        @Override
        public boolean test(AttachmentEntity newEntity) {
            return oldEntity.getName().equals(newEntity.getName())
                && oldEntity.getType().equals(newEntity.getType())
                && oldEntity.getParentId().equals(newEntity.getParentId())
                && oldEntity.getSize().equals(newEntity.getSize());
        }
    }


    @Test
    void saveEntity() {
        AttachmentEntity entity = AttachmentEntity.builder()
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .name(RandomUtils.randomString(20))
            .type(AttachmentType.File)
            .updateTime(LocalDateTime.now())
            .size(RandomUtils.getRandom().nextLong())
            .build();

        StepVerifier.create(attachmentService.saveEntity(entity))
            .expectNextMatches(new AttachmentEntityPredicate(entity))
            .verifyComplete();

        StepVerifier.create(attachmentRepository.findByTypeAndParentIdAndName(
                entity.getType(), entity.getParentId(), entity.getName()
            )).expectNextMatches(new AttachmentEntityPredicate(entity))
            .verifyComplete();
    }

    @Test
    void save() {
    }

    @Test
    void testUpload() {
    }

    @Test
    void listEntitiesByCondition() {
    }

    @Test
    void listByCondition() {
    }

    @Test
    void findById() {
    }

    @Test
    void findByTypeAndParentIdAndName() {
    }

    @Test
    void removeById() {
    }

    @Test
    void removeByIdForcibly() {
    }

    @Test
    void removeByTypeAndParentIdAndName() {
    }

    @Test
    void receiveAndHandleFragmentUploadChunkFile() {
    }

    @Test
    void revertFragmentUploadFile() {
    }

    @Test
    void createDirectory() {
    }

    @Test
    void findAttachmentPathDirsById() {
    }

    @Test
    void existsByParentIdAndName() {
    }

    @Test
    void existsByTypeAndParentIdAndName() {
    }
}