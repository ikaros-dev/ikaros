package run.ikaros.server.core.attachment.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.server.core.attachment.event.AttachmentReferenceSaveEvent;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.AttachmentReferenceEntity;
import run.ikaros.server.store.repository.AttachmentReferenceRepository;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.EpisodeRepository;

class AttachmentReferenceServiceImplTest {

    @Mock
    private AttachmentReferenceRepository repository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;
    private AttachmentReferenceServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new AttachmentReferenceServiceImpl(
            repository, attachmentRepository, episodeRepository, applicationEventPublisher);
    }

    @Test
    void save_nonEpisodeType_publishesEvent() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID referenceId = UuidV7Utils.generateUuid();

        AttachmentReference attachmentReference = AttachmentReference.builder()
            .type(AttachmentReferenceType.SUBJECT)
            .attachmentId(attachmentId)
            .referenceId(referenceId)
            .build();

        AttachmentReferenceEntity savedEntity = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.SUBJECT)
            .attachmentId(attachmentId)
            .referenceId(referenceId)
            .build();

        when(repository.save(any(AttachmentReferenceEntity.class)))
            .thenReturn(Mono.just(savedEntity));

        StepVerifier.create(service.save(attachmentReference))
            .assertNext(ref -> {
                assertThat(ref.getAttachmentId()).isEqualTo(attachmentId);
                assertThat(ref.getReferenceId()).isEqualTo(referenceId);
                assertThat(ref.getType()).isEqualTo(AttachmentReferenceType.SUBJECT);
            })
            .verifyComplete();

        ArgumentCaptor<AttachmentReferenceSaveEvent> eventCaptor =
            ArgumentCaptor.forClass(AttachmentReferenceSaveEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEntity()).isEqualTo(savedEntity);
    }

    @Test
    void save_episodeType_checksAttachmentExists() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID referenceId = UuidV7Utils.generateUuid();

        AttachmentReference attachmentReference = AttachmentReference.builder()
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attachmentId)
            .referenceId(referenceId)
            .build();

        AttachmentEntity attachmentEntity = AttachmentEntity.builder()
            .id(attachmentId)
            .name("test-video.mp4")
            .type(run.ikaros.api.store.enums.AttachmentType.File)
            .build();

        AttachmentReferenceEntity savedEntity = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attachmentId)
            .referenceId(referenceId)
            .build();

        when(attachmentRepository.findById(attachmentId))
            .thenReturn(Mono.just(attachmentEntity));
        when(repository.save(any(AttachmentReferenceEntity.class)))
            .thenReturn(Mono.just(savedEntity));

        StepVerifier.create(service.save(attachmentReference))
            .assertNext(ref -> {
                assertThat(ref.getAttachmentId()).isEqualTo(attachmentId);
                assertThat(ref.getReferenceId()).isEqualTo(referenceId);
                assertThat(ref.getType()).isEqualTo(AttachmentReferenceType.EPISODE);
            })
            .verifyComplete();

        verify(attachmentRepository).findById(attachmentId);
        verify(applicationEventPublisher).publishEvent(any(AttachmentReferenceSaveEvent.class));
    }

    @Test
    void save_episodeType_attachmentNotFound() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID referenceId = UuidV7Utils.generateUuid();

        AttachmentReference attachmentReference = AttachmentReference.builder()
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attachmentId)
            .referenceId(referenceId)
            .build();

        when(attachmentRepository.findById(attachmentId))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.save(attachmentReference))
            .verifyError();
    }

    @Test
    void save_nullReference_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.save(null));
    }

    @Test
    void findAllByTypeAndAttachmentId() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID referenceId = UuidV7Utils.generateUuid();

        AttachmentReferenceEntity entity = AttachmentReferenceEntity.builder()
            .id(UuidV7Utils.generateUuid())
            .type(AttachmentReferenceType.EPISODE)
            .attachmentId(attachmentId)
            .referenceId(referenceId)
            .build();

        when(repository.findAllByTypeAndAttachmentId(
            AttachmentReferenceType.EPISODE, attachmentId))
            .thenReturn(Flux.just(entity));

        StepVerifier.create(service.findAllByTypeAndAttachmentId(
                AttachmentReferenceType.EPISODE, attachmentId))
            .assertNext(ref -> {
                assertThat(ref.getAttachmentId()).isEqualTo(attachmentId);
                assertThat(ref.getReferenceId()).isEqualTo(referenceId);
                assertThat(ref.getType()).isEqualTo(AttachmentReferenceType.EPISODE);
            })
            .verifyComplete();
    }

    @Test
    void findAllByTypeAndAttachmentId_nullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.findAllByTypeAndAttachmentId(null, UuidV7Utils.generateUuid()));
    }

    @Test
    void removeById() {
        UUID id = UuidV7Utils.generateUuid();
        when(repository.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.removeById(id))
            .verifyComplete();

        verify(repository).deleteById(id);
    }

    @Test
    void removeAllByTypeAndReferenceId() {
        UUID referenceId = UuidV7Utils.generateUuid();
        when(repository.deleteAllByTypeAndReferenceId(
            AttachmentReferenceType.SUBJECT, referenceId))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.removeAllByTypeAndReferenceId(
                AttachmentReferenceType.SUBJECT, referenceId))
            .verifyComplete();

        verify(repository).deleteAllByTypeAndReferenceId(
            AttachmentReferenceType.SUBJECT, referenceId);
    }

    @Test
    void removeAllByTypeAndReferenceId_nullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.removeAllByTypeAndReferenceId(null, UuidV7Utils.generateUuid()));
    }

    @Test
    void removeByTypeAndAttachmentIdAndReferenceId() {
        UUID attachmentId = UuidV7Utils.generateUuid();
        UUID referenceId = UuidV7Utils.generateUuid();
        when(repository.deleteByTypeAndAttachmentIdAndReferenceId(
            AttachmentReferenceType.EPISODE, attachmentId, referenceId))
            .thenReturn(Mono.empty());

        StepVerifier.create(service.removeByTypeAndAttachmentIdAndReferenceId(
                AttachmentReferenceType.EPISODE, attachmentId, referenceId))
            .verifyComplete();

        verify(repository).deleteByTypeAndAttachmentIdAndReferenceId(
            AttachmentReferenceType.EPISODE, attachmentId, referenceId);
    }

    @Test
    void removeByTypeAndAttachmentIdAndReferenceId_nullType_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.removeByTypeAndAttachmentIdAndReferenceId(
                null, UuidV7Utils.generateUuid(), UuidV7Utils.generateUuid()));
    }
}
