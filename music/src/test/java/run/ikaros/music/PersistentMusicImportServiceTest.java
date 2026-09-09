package run.ikaros.music;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;
import run.ikaros.storage.api.AttachmentKind;
import run.ikaros.storage.api.AttachmentView;
import run.ikaros.storage.api.StorageService;

class PersistentMusicImportServiceTest {
    @Test void importsAudioAttachmentAsTrackAndAudioSource() {
        UUID owner = UUID.randomUUID(); UUID attachmentId = UUID.randomUUID(); UUID resourceId = UUID.randomUUID(); UUID trackId = UUID.randomUUID(); UUID importId = UUID.randomUUID();
        StorageService storage = org.mockito.Mockito.mock(StorageService.class); MusicTrackRepository tracks = org.mockito.Mockito.mock(MusicTrackRepository.class); MusicAudioSourceService sources = org.mockito.Mockito.mock(MusicAudioSourceService.class); MusicImportRepository imports = org.mockito.Mockito.mock(MusicImportRepository.class); TransactionalOperator transaction = org.mockito.Mockito.mock(TransactionalOperator.class);
        AttachmentView attachment = new AttachmentView(attachmentId, resourceId, "song.mp3", AttachmentKind.ORIGINAL, "sha", 100, "audio/mpeg", AttachmentAvailabilityStatus.READY);
        MusicTrackEntity track = new MusicTrackEntity(trackId, owner, resourceId, "song", 1234L, null, false, 0L);
        MusicImportEntity imported = new MusicImportEntity(importId, owner, attachmentId, trackId, "SUCCEEDED", null, null, "key", Instant.now(), Instant.now(), 0L);
        when(storage.get(owner, attachmentId)).thenReturn(Mono.just(attachment)); when(tracks.findByOwnerIdAndResourceId(owner, resourceId)).thenReturn(Mono.empty()); when(tracks.save(any())).thenReturn(Mono.just(track)); when(sources.add(any(), any(), any())).thenReturn(Mono.just(new MusicAudioSourceView(UUID.randomUUID(), trackId, attachmentId, "MP3", "MPEG", 1234L, null, null, null, null, false, "AVAILABLE", 0))); when(imports.save(any())).thenReturn(Mono.just(imported)); when(imports.findByOwnerIdAndIdempotencyKey(owner, "key")).thenReturn(Mono.empty());
        when(transaction.transactional(any(Mono.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StepVerifier.create(new PersistentMusicImportService(storage, tracks, sources, imports, transaction).create(owner, new CreateMusicImportRequest(attachmentId, "song", 1234L, "MP3", "MPEG", null, null, null, null, false), "key"))
            .expectNextMatches(value -> value.trackId().equals(trackId) && "SUCCEEDED".equals(value.status())).verifyComplete();
    }
    @Test void rejectsRepeatedAttachmentAsConflict() { UUID owner=UUID.randomUUID(),attachmentId=UUID.randomUUID(),resourceId=UUID.randomUUID(); StorageService storage=org.mockito.Mockito.mock(StorageService.class); MusicTrackRepository tracks=org.mockito.Mockito.mock(MusicTrackRepository.class); MusicAudioSourceService sources=org.mockito.Mockito.mock(MusicAudioSourceService.class); MusicImportRepository imports=org.mockito.Mockito.mock(MusicImportRepository.class); TransactionalOperator transaction=org.mockito.Mockito.mock(TransactionalOperator.class); AttachmentView attachment=new AttachmentView(attachmentId,resourceId,"song.mp3",AttachmentKind.ORIGINAL,"sha",100,"audio/mpeg",AttachmentAvailabilityStatus.READY); when(storage.get(owner,attachmentId)).thenReturn(Mono.just(attachment)); when(imports.findByOwnerIdAndIdempotencyKey(owner,"repeat")).thenReturn(Mono.empty()); when(tracks.findByOwnerIdAndResourceId(owner,resourceId)).thenReturn(Mono.just(new MusicTrackEntity(UUID.randomUUID(),owner,resourceId,"existing",1L,null,false,0L))); when(transaction.transactional(any(Mono.class))).thenAnswer(i->i.getArgument(0)); StepVerifier.create(new PersistentMusicImportService(storage,tracks,sources,imports,transaction).create(owner,new CreateMusicImportRequest(attachmentId,null,null,null,null,null,null,null,null,false),"repeat")).expectError(run.ikaros.common.ConflictException.class).verify(); }
}
