package run.ikaros.music;
import java.time.Instant; import java.util.UUID;
public record MusicPlaylistEntryView(UUID id, UUID playlistId, UUID trackId, int position, Instant addedAt) {}
