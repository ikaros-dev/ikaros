package run.ikaros.storage;

/** Provider 返回的完整性校验结果。 */
public record BlobIntegrityResult(BlobIntegrityStatus status, String actualSha256, long actualSize) {
}
