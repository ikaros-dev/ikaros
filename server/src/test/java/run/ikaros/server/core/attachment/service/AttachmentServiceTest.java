package run.ikaros.server.core.attachment.service;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
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
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
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

    @Test
    void upload() throws IOException {
        StepVerifier.create(attachmentService.listEntitiesByCondition(
                    AttachmentSearchCondition.builder().build())
                .map(PagingWrap::isEmpty))
            .expectNext(true)
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


}