package run.ikaros.music;
import java.time.Instant; import java.util.UUID;
public record MusicPlaylistView(UUID id, String name, String description, Instant createdAt, Instant updatedAt) {}
