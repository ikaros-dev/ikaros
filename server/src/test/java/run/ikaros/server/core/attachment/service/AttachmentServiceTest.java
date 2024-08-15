package run.ikaros.server.core.attachment.service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        // parent id is null
        final var attachment = Attachment.builder()
            .name(RandomUtils.randomString(20))
            .type(AttachmentType.File)
            .build();

        StepVerifier.create(attachmentService.save(attachment))
            .expectNextMatches(newAttachment ->
                attachment.getName().equals(newAttachment.getName())
                    && attachment.getType().equals(newAttachment.getType()))
            .verifyComplete();

        StepVerifier.create(attachmentRepository.findByTypeAndParentIdAndName(
                attachment.getType(), attachment.getParentId(), attachment.getName()
            )).expectNextMatches(newAttachmentEntity ->
                attachment.getType().equals(newAttachmentEntity.getType())
                    && attachment.getParentId().equals(newAttachmentEntity.getParentId())
                    && attachment.getName().equals(newAttachmentEntity.getName()))
            .verifyComplete();

        // parent id not null
        final var attachmentPidNotNull = Attachment.builder()
            .name(RandomUtils.randomString(20))
            .type(AttachmentType.File)
            .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .build();

        StepVerifier.create(attachmentService.save(attachmentPidNotNull))
            .expectNextMatches(newAttachment ->
                attachmentPidNotNull.getParentId().equals(newAttachment.getParentId())
                    && attachmentPidNotNull.getName().equals(newAttachment.getName())
                    && attachmentPidNotNull.getType().equals(newAttachment.getType()))
            .verifyComplete();

        StepVerifier.create(attachmentRepository.findByTypeAndParentIdAndName(
                attachmentPidNotNull.getType(), attachmentPidNotNull.getParentId(),
                attachmentPidNotNull.getName()
            )).expectNextMatches(newAttachmentEntity ->
                attachmentPidNotNull.getType().equals(newAttachmentEntity.getType())
                    && attachmentPidNotNull.getParentId().equals(newAttachmentEntity.getParentId())
                    && attachmentPidNotNull.getName().equals(newAttachmentEntity.getName()))
            .verifyComplete();
    }

    @Test
    void testUpload() {
    }

    private boolean entitiesIsEquals(List<AttachmentEntity> oldAttachments,
                                     List<AttachmentEntity> newAttachments) {
        if (oldAttachments.size() != newAttachments.size()) {
            return false;
        }

        oldAttachments.sort((o1, o2) -> (int) (o1.getId() - o2.getId()));
        newAttachments.sort((o1, o2) -> (int) (o1.getId() - o2.getId()));

        boolean result = true;
        for (int i = 0; i < oldAttachments.size(); i++) {
            AttachmentEntity oldEntity = oldAttachments.get(i);
            AttachmentEntity newEntity = newAttachments.get(i);
            if (!oldEntity.getName().equals(newEntity.getName())) {
                result = false;
                break;
            }
            if (!oldEntity.getType().equals(newEntity.getType())) {
                result = false;
                break;
            }
            if (!oldEntity.getParentId().equals(newEntity.getParentId())) {
                result = false;
                break;
            }
            if (!oldEntity.getSize().equals(newEntity.getSize())) {
                result = false;
                break;
            }
        }
        return result;
    }

    private boolean attIsEquals(List<AttachmentEntity> oldAttachments,
                                List<Attachment> newAttachments) {
        ArrayList<AttachmentEntity> objects = new ArrayList<>();
        for (Attachment newAttachment : newAttachments) {
            objects.add(AttachmentEntity.builder()
                .id(newAttachment.getId())
                .name(newAttachment.getName())
                .type(newAttachment.getType())
                .parentId(newAttachment.getParentId())
                .size(newAttachment.getSize())
                .updateTime(newAttachment.getUpdateTime())
                .build());
        }
        return entitiesIsEquals(oldAttachments, objects);
    }

    @Test
    void listEntitiesByCondition() {
        final int size = RandomUtils.getRandom().nextInt(1, 100);
        List<AttachmentEntity> attachmentEntities = new ArrayList<>(size);

        final String namePrefix = RandomUtils.randomString(20);
        for (int i = 0; i < size; i++) {
            AttachmentEntity entity = AttachmentEntity.builder()
                .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
                .type(AttachmentType.File)
                .name(namePrefix + i)
                .size(Long.parseLong(String.valueOf(i)))
                .build();
            attachmentEntities.add(entity);
            StepVerifier.create(attachmentService.saveEntity(entity))
                .expectNextMatches(new AttachmentEntityPredicate(entity))
                .verifyComplete();
        }

        AttachmentSearchCondition condition = AttachmentSearchCondition.builder()
            .page(1).size(size).parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .type(AttachmentType.File)
            .build();
        StepVerifier.create(attachmentService.listEntitiesByCondition(condition))
            .expectNextMatches(newPagingwrap ->
                condition.getSize().equals(newPagingwrap.getSize())
                    && condition.getPage().equals(newPagingwrap.getPage())
                    && condition.getSize().equals(newPagingwrap.getItems().size())
                    &&
                    entitiesIsEquals(attachmentEntities.subList(0, size), newPagingwrap.getItems()))
            .verifyComplete();

    }

    @Test
    void listByCondition() {

        final int size = RandomUtils.getRandom().nextInt(1, 100);
        List<AttachmentEntity> attachmentEntities = new ArrayList<>(size);

        final String namePrefix = RandomUtils.randomString(20);
        for (int i = 0; i < size; i++) {
            AttachmentEntity entity = AttachmentEntity.builder()
                .parentId(AttachmentConst.ROOT_DIRECTORY_ID)
                .type(AttachmentType.File)
                .name(namePrefix + i)
                .size(Long.parseLong(String.valueOf(i)))
                .build();
            attachmentEntities.add(entity);
            StepVerifier.create(attachmentService.saveEntity(entity))
                .expectNextMatches(new AttachmentEntityPredicate(entity))
                .verifyComplete();
        }

        AttachmentSearchCondition condition = AttachmentSearchCondition.builder()
            .page(1).size(size).parentId(AttachmentConst.ROOT_DIRECTORY_ID)
            .type(AttachmentType.File)
            .build();
        StepVerifier.create(attachmentService.listByCondition(condition))
            .expectNextMatches(newPagingwrap ->
                condition.getSize().equals(newPagingwrap.getSize())
                    && condition.getPage().equals(newPagingwrap.getPage())
                    && condition.getSize().equals(newPagingwrap.getItems().size())
                    &&
                    attIsEquals(attachmentEntities.subList(0, size), newPagingwrap.getItems()))
            .verifyComplete();

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