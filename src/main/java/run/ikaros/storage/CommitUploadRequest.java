package run.ikaros.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Provider 上传完成后提交的内容身份与物理位置确认。 */
public record CommitUploadRequest(
    @NotBlank @Size(max = 128) String sha256,
    @PositiveOrZero long sizeBytes,
    @NotBlank @Size(max = 256) String mediaType,
    @NotBlank @Size(max = 512) String fileName,
    @NotNull AttachmentKind kind,
    @NotBlank @Size(max = 128) String provider,
    @NotNull StorageTier tier,
    @NotBlank @Size(max = 1024) String objectKey
) {
    AttachBlobRequest asAttachment() {
        return new AttachBlobRequest(sha256, sizeBytes, mediaType, fileName, kind, provider, tier, objectKey);
    }
}
