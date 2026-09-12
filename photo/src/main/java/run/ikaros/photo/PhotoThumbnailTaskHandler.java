package run.ikaros.photo;

import jakarta.annotation.PostConstruct;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.common.ConflictException;
import run.ikaros.operations.api.BackgroundTask;
import run.ikaros.operations.api.BackgroundTaskDispatcher;
import run.ikaros.storage.api.AttachmentContentService;
import run.ikaros.storage.api.AttachmentView;
import run.ikaros.storage.api.StorageService;

/** photo.thumbnail 的后台 Handler；原图读取和 ImageIO 均隔离在 bounded-elastic。 */
@Component
public final class PhotoThumbnailTaskHandler {
    private static final int MAX_EDGE = 512;
    private static final int MAX_SOURCE_BYTES = 256 * 1024 * 1024;

    private final BackgroundTaskDispatcher dispatcher;
    private final AttachmentContentService content;
    private final StorageService storage;
    private final PhotoAssetRepository assets;

    public PhotoThumbnailTaskHandler(BackgroundTaskDispatcher dispatcher, AttachmentContentService content,
                                     StorageService storage, PhotoAssetRepository assets) {
        this.dispatcher = dispatcher;
        this.content = content;
        this.storage = storage;
        this.assets = assets;
    }

    @PostConstruct
    void register() { dispatcher.register("photo.thumbnail", this::handle); }

    Mono<Map<String, Object>> handle(BackgroundTask task) {
        UUID owner = uuid(task, "owner_id");
        UUID photoId = uuid(task, "photo_id");
        UUID sourceId = uuid(task, "source_attachment_id");
        return storage.get(owner, sourceId)
            .filter(this::isImage)
            .switchIfEmpty(Mono.error(new ConflictException("原图附件不可用或不是图片")))
            .flatMap(source -> createThumbnail(owner, photoId, sourceId, source, task.id()));
    }

    private Mono<Map<String, Object>> createThumbnail(UUID owner, UUID photoId, UUID sourceId,
                                                       AttachmentView source, UUID taskId) {
        return Mono.usingWhen(
            Mono.fromCallable(() -> Files.createTempFile("ikaros-photo-", "." + extension(source.mediaType())))
                .subscribeOn(Schedulers.boundedElastic()),
            path -> DataBufferUtils.write(content.read(owner, sourceId), path)
                .then(Mono.fromCallable(() -> render(path)).subscribeOn(Schedulers.boundedElastic()))
                .flatMap(bytes -> storage.writeDerived(owner, source.resourceId(), sourceId,
                    "thumbnail-" + source.fileName(), "image/jpeg", bytes))
                .flatMap(derived -> assets.save(new PhotoAssetEntity(null, owner, photoId, derived.id(),
                    PhotoAssetRole.THUMBNAIL, false, "AVAILABLE", null)))
                .map(asset -> Map.of("photo_id", photoId.toString(), "source_attachment_id", sourceId.toString(),
                    "thumbnail_attachment_id", asset.attachmentId().toString(), "task_id", taskId.toString())),
            path -> Mono.fromRunnable(() -> delete(path)).subscribeOn(Schedulers.boundedElastic()));
    }

    private byte[] render(Path path) throws Exception {
        if (Files.size(path) > MAX_SOURCE_BYTES) throw new ConflictException("原图超过缩略图处理上限");
        BufferedImage input = ImageIO.read(path.toFile());
        if (input == null) throw new ConflictException("原图格式不受支持");
        double scale = Math.min(1D, Math.min((double) MAX_EDGE / input.getWidth(), (double) MAX_EDGE / input.getHeight()));
        int width = Math.max(1, (int) Math.round(input.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(input.getHeight() * scale));
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = output.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(input, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(Math.min(1024 * 1024, width * height));
        if (!ImageIO.write(output, "jpeg", bytes)) throw new ConflictException("无法生成 JPEG 缩略图");
        return bytes.toByteArray();
    }

    private boolean isImage(AttachmentView attachment) {
        return attachment.mediaType() != null && attachment.mediaType().toLowerCase(java.util.Locale.ROOT).startsWith("image/")
            && attachment.availability() == run.ikaros.storage.api.AttachmentAvailabilityStatus.READY;
    }

    private UUID uuid(BackgroundTask task, String key) { return UUID.fromString(String.valueOf(task.payload().get(key))); }
    private String extension(String mediaType) { return mediaType.substring(mediaType.indexOf('/') + 1).replaceAll("[^A-Za-z0-9]", ""); }
    private void delete(Path path) { try { Files.deleteIfExists(path); } catch (Exception ignored) { } }
}
