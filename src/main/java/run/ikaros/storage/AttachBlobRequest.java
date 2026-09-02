package run.ikaros.storage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 在已由上传或导入流程写入存储后，为 Resource 登记 Attachment 的请求数据。
 */
public record AttachBlobRequest(
    @NotBlank @Pattern(regexp = "^[A-Fa-f0-9]{64}$") String sha256,
    @Min(0) long sizeBytes,
    @Size(max = 256) String mediaType,
    @NotBlank @Size(max = 512) String fileName,
    @NotNull AttachmentKind kind,
    @NotBlank @Size(max = 128) String provider,
    @NotNull StorageTier tier,
    @NotBlank @Size(max = 1024) String objectKey
) {
}
