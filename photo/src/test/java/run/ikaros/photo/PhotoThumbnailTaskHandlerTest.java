package run.ikaros.photo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.BackgroundTaskDispatcher;
import run.ikaros.operations.api.TaskStatus;
import run.ikaros.storage.api.AttachmentAvailabilityStatus;
import run.ikaros.storage.api.AttachmentContentService;
import run.ikaros.storage.api.AttachmentKind;
import run.ikaros.storage.api.AttachmentView;
import run.ikaros.storage.api.StorageService;

class PhotoThumbnailTaskHandlerTest {
    @Test
    void rendersPngAsJpegAndRegistersThumbnailAsset() throws Exception {
        UUID owner = UUID.randomUUID(); UUID resourceId = UUID.randomUUID();
        UUID photoId = UUID.randomUUID(); UUID sourceId = UUID.randomUUID(); UUID derivedId = UUID.randomUUID();
        byte[] png = image("png", 1200, 600);
        BackgroundTaskDispatcher dispatcher = mock(BackgroundTaskDispatcher.class);
        AttachmentContentService content = mock(AttachmentContentService.class);
        StorageService storage = mock(StorageService.class);
        PhotoAssetRepository assets = mock(PhotoAssetRepository.class);
        AttachmentView source = new AttachmentView(sourceId, resourceId, "source.png", AttachmentKind.ORIGINAL,
            "a".repeat(64), png.length, "image/png", AttachmentAvailabilityStatus.READY);
        AttachmentView derived = new AttachmentView(derivedId, resourceId, "thumbnail-source.png", AttachmentKind.DERIVED,
            "b".repeat(64), 123, "image/jpeg", AttachmentAvailabilityStatus.READY);
        when(storage.get(owner, sourceId)).thenReturn(Mono.just(source));
        when(content.read(owner, sourceId)).thenReturn(Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(png)));
        when(storage.writeDerived(any(), any(), any(), any(), any(), any())).thenReturn(Mono.just(derived));
        when(assets.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        PhotoThumbnailTaskHandler handler = new PhotoThumbnailTaskHandler(dispatcher, content, storage, assets);
        BackgroundTask task = new BackgroundTask(UUID.randomUUID(), "photo.thumbnail", TaskStatus.RUNNING,
            Map.of("owner_id", owner.toString(), "photo_id", photoId.toString(), "source_attachment_id", sourceId.toString()),
            "photo-thumbnail:" + photoId, null, null, "runner", UUID.randomUUID(), null, 1, null, Map.of(), Map.of(), null, null, null);

        StepVerifier.create(handler.handle(task))
            .assertNext(result -> assertThat(result.get("thumbnail_attachment_id")).isEqualTo(derivedId.toString()))
            .verifyComplete();
        verify(storage).writeDerived(any(), any(), any(), any(), any(), any());
        verify(assets).save(any(PhotoAssetEntity.class));
    }

    private byte[] image(String format, int width, int height) throws Exception {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) for (int y = 0; y < height; y++) image.setRGB(x, y, Color.ORANGE.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
