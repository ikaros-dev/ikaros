package run.ikaros.photo; import java.time.Instant; import java.util.UUID; public record PhotoAlbumView(UUID id,String name,String description,Instant createdAt,Instant updatedAt) {}
