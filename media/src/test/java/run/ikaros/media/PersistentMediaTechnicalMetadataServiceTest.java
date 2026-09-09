package run.ikaros.media;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.storage.api.AttachmentReference;
import run.ikaros.storage.api.AttachmentReferenceQuery;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentMediaTechnicalMetadataServiceTest {
    @Test
    void associatesSubtitleOnlyAfterReleaseAndAttachmentAuthorization() {
        UUID owner = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        MediaReleaseRepository releases = org.mockito.Mockito.mock(MediaReleaseRepository.class);
        MediaProbeRepository probes = org.mockito.Mockito.mock(MediaProbeRepository.class);
        MediaExternalSubtitleRepository subtitles = org.mockito.Mockito.mock(MediaExternalSubtitleRepository.class);
        AttachmentReferenceQuery attachments = org.mockito.Mockito.mock(AttachmentReferenceQuery.class);
        when(releases.findById(releaseId)).thenReturn(Mono.just(new MediaReleaseEntity(releaseId, owner, resourceId, UUID.randomUUID(),
            null, null, MediaReleaseState.AVAILABLE, null, Instant.now(), Instant.now(), 0L)));
        when(attachments.requireActiveForResource(owner, resourceId, attachmentId)).thenReturn(Mono.just(new AttachmentReference(attachmentId, resourceId)));
        when(subtitles.save(any(MediaExternalSubtitleEntity.class))).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(new PersistentMediaTechnicalMetadataService(releases, probes, subtitles, attachments).addSubtitle(owner, releaseId,
                new AddExternalSubtitleRequest(attachmentId, "zh-CN", "中文字幕", "SRT", "local", 0, false, false)))
            .expectNextMatches(view -> view.releaseId().equals(releaseId) && view.attachmentId().equals(attachmentId)
                && view.language().equals("zh-CN"))
            .verifyComplete();
        verify(subtitles).save(any(MediaExternalSubtitleEntity.class));
    }

    @Test
    void doesNotSaveSubtitleForUnknownRelease() {
        UUID owner = UUID.randomUUID();
        UUID releaseId = UUID.randomUUID();
        MediaReleaseRepository releases = org.mockito.Mockito.mock(MediaReleaseRepository.class);
        MediaExternalSubtitleRepository subtitles = org.mockito.Mockito.mock(MediaExternalSubtitleRepository.class);
        when(releases.findById(releaseId)).thenReturn(Mono.empty());

        StepVerifier.create(new PersistentMediaTechnicalMetadataService(releases, org.mockito.Mockito.mock(MediaProbeRepository.class),
                subtitles, org.mockito.Mockito.mock(AttachmentReferenceQuery.class)).addSubtitle(owner, releaseId,
                new AddExternalSubtitleRequest(UUID.randomUUID(), "zh-CN", "字幕", "SRT", null, 0, false, false)))
            .expectErrorMessage("Media Release 不存在")
            .verify();
        verify(subtitles, never()).save(any(MediaExternalSubtitleEntity.class));
    }
}
