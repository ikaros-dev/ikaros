package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentDriverFetcher;
import run.ikaros.api.core.media.MediaFileDetectionResult;
import run.ikaros.api.core.media.MediaFileFormat;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.service.AttachmentMediaValidationService;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.attachment.service.ValidatedMediaStream;
import run.ikaros.server.store.entity.AttachmentEntity;

/** 附件内容检查服务测试。 */
class DefaultAttachmentContentInspectionServiceTest {

    @Mock
    private AttachmentService attachmentService;
    @Mock
    private AttachmentMediaValidationService mediaValidationService;
    @Mock
    private AttachmentDriverFetcher fetcher;
    private DefaultAttachmentContentInspectionService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefaultAttachmentContentInspectionService(
            attachmentService, mediaValidationService);
    }

    @Test
    void inspectRejectsUnsupportedNameBeforeDriverStream() {
        Attachment attachment = Attachment.builder()
            .id(UUID.randomUUID()).type(AttachmentType.Driver_File)
            .name("payload.exe").build();
        doThrow(new IllegalArgumentException("unsupported"))
            .when(mediaValidationService).validateFilename("payload.exe");

        StepVerifier.create(service.inspect(attachment, fetcher))
            .expectError(IllegalArgumentException.class)
            .verify();

        verify(fetcher, never()).getSteam(any(Attachment.class));
        verify(mediaValidationService).validateFilename("payload.exe");
    }

    @Test
    void inspectReturnsDetectionAndReleasesReplayPrefix() {
        Attachment attachment = Attachment.builder()
            .id(UUID.randomUUID()).type(AttachmentType.Driver_File)
            .name("episode.mp4").build();
        MediaFileDetectionResult detection = new MediaFileDetectionResult(MediaFileFormat.MP4);
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(new byte[] {0, 1, 2});
        when(mediaValidationService.validate(any(Flux.class), eq("episode.mp4")))
            .thenReturn(Mono.just(new ValidatedMediaStream(detection, Flux.just(buffer))));
        when(fetcher.getSteam(attachment)).thenReturn(Flux.empty());

        StepVerifier.create(service.inspect(attachment, fetcher))
            .assertNext(result -> assertThat(result).isEqualTo(detection))
            .verifyComplete();

        verify(fetcher).getSteam(attachment);
    }

    @Test
    void inspectEntityRejectsUnsupportedNameBeforeAttachmentStream() {
        AttachmentEntity entity = AttachmentEntity.builder()
            .id(UUID.randomUUID()).type(AttachmentType.File)
            .name("payload.zip").build();
        doThrow(new IllegalArgumentException("unsupported"))
            .when(mediaValidationService).validateFilename("payload.zip");

        StepVerifier.create(service.inspect(entity))
            .expectError(IllegalArgumentException.class)
            .verify();

        verify(attachmentService, never()).getStreamByIdWithoutRange(any(UUID.class));
    }
}
