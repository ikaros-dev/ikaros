package run.ikaros.photo;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;
import run.ikaros.resource.api.ResourceView;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;
import run.ikaros.storage.api.AttachmentKind;
import run.ikaros.storage.api.AttachmentReference;
import run.ikaros.storage.api.AttachmentReferenceQuery;
import run.ikaros.storage.api.AttachmentView;
import run.ikaros.storage.api.StorageService;
import run.ikaros.operations.api.BackgroundTaskService;

class PersistentPhotoServiceTest {
    @Test
    void bindsReadyImageAttachmentToOwnedPhotoResource() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID();
        ResourceService resources = mock(ResourceService.class);
        PhotoRepository photos = mock(PhotoRepository.class);
        PhotoAssetRepository assets = mock(PhotoAssetRepository.class);
        PhotoAlbumRepository albums = mock(PhotoAlbumRepository.class);
        PhotoAlbumMemberRepository members = mock(PhotoAlbumMemberRepository.class);
        AttachmentReferenceQuery references = mock(AttachmentReferenceQuery.class);
        StorageService storage = mock(StorageService.class);
        BackgroundTaskService tasks = mock(BackgroundTaskService.class);
        ResourceView resource = mock(ResourceView.class);
        PhotoEntity photo = new PhotoEntity(photoId, owner, resourceId, null, null, null, null, null, null, null, null, null, null, null, 0L);

        when(references.requireReadable(owner, attachmentId)).thenReturn(Mono.just(new AttachmentReference(attachmentId, resourceId)));
        when(storage.get(owner, attachmentId)).thenReturn(Mono.just(new AttachmentView(attachmentId, resourceId, "photo.jpg", AttachmentKind.ORIGINAL, "hash", 42, "image/jpeg", AttachmentAvailabilityStatus.READY)));
        when(resources.get(owner, resourceId)).thenReturn(Mono.just(resource));
        when(resource.id()).thenReturn(resourceId);
        when(resource.type()).thenReturn(ResourceType.PHOTO);
        when(photos.findByOwnerIdAndResourceId(owner, resourceId)).thenReturn(Mono.empty());
        when(photos.save(any())).thenReturn(Mono.just(photo));
        when(assets.save(any())).thenReturn(Mono.just(mock(PhotoAssetEntity.class)));

        StepVerifier.create(new PersistentPhotoService(resources, photos, assets, albums, members, references, storage, tasks)
                .create(owner, new CreatePhotoRequest("Sunset", attachmentId, "zh-CN")))
            .expectNextMatches(view -> view.id().equals(photoId) && view.resourceId().equals(resourceId))
            .verifyComplete();
    }

    @Test
    void rejectsNonImageAttachmentBeforeCreatingPhoto() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID attachmentId = UUID.randomUUID();
        ResourceService resources = mock(ResourceService.class);
        PhotoRepository photos = mock(PhotoRepository.class);
        PhotoAssetRepository assets = mock(PhotoAssetRepository.class);
        PhotoAlbumRepository albums = mock(PhotoAlbumRepository.class);
        PhotoAlbumMemberRepository members = mock(PhotoAlbumMemberRepository.class);
        AttachmentReferenceQuery references = mock(AttachmentReferenceQuery.class);
        StorageService storage = mock(StorageService.class);
        BackgroundTaskService tasks = mock(BackgroundTaskService.class);

        when(references.requireReadable(owner, attachmentId)).thenReturn(Mono.just(new AttachmentReference(attachmentId, resourceId)));
        when(storage.get(owner, attachmentId)).thenReturn(Mono.just(new AttachmentView(attachmentId, resourceId, "notes.txt", AttachmentKind.ORIGINAL, "hash", 42, "text/plain", AttachmentAvailabilityStatus.READY)));

        StepVerifier.create(new PersistentPhotoService(resources, photos, assets, albums, members, references, storage, tasks)
                .create(owner, new CreatePhotoRequest("Not photo", attachmentId, "en-US")))
            .expectError(run.ikaros.common.ConflictException.class)
            .verify();
        verify(resources, never()).get(any(), any());
        verify(photos, never()).save(any());
    }

    @Test
    void rejectsAlbumUpdateWhenVersionIsStale() {
        UUID owner = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        ResourceService resources = mock(ResourceService.class);
        PhotoRepository photos = mock(PhotoRepository.class);
        PhotoAssetRepository assets = mock(PhotoAssetRepository.class);
        PhotoAlbumRepository albums = mock(PhotoAlbumRepository.class);
        PhotoAlbumMemberRepository members = mock(PhotoAlbumMemberRepository.class);
        AttachmentReferenceQuery references = mock(AttachmentReferenceQuery.class);
        StorageService storage = mock(StorageService.class);
        BackgroundTaskService tasks = mock(BackgroundTaskService.class);
        when(albums.findById(albumId)).thenReturn(Mono.just(new PhotoAlbumEntity(albumId, owner, "Old", null, Instant.now(), Instant.now(), null, 2L)));

        StepVerifier.create(new PersistentPhotoService(resources, photos, assets, albums, members, references, storage, tasks)
                .updateAlbum(owner, albumId, new UpdatePhotoAlbumRequest("New", null), 1L))
            .expectError(run.ikaros.common.PreconditionFailedException.class)
            .verify();
        verify(albums, never()).save(any());
    }
}
