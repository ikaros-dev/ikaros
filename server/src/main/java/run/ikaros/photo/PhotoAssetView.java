package run.ikaros.photo; import java.util.UUID; public record PhotoAssetView(UUID id,UUID photoId,UUID attachmentId,PhotoAssetRole role,boolean primary,String availability) {}
