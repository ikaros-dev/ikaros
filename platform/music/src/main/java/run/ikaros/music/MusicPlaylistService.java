package run.ikaros.music;
import java.util.UUID; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
public interface MusicPlaylistService {
    Mono<MusicPlaylistView> create(UUID ownerId, CreateMusicPlaylistRequest request);
    Flux<MusicPlaylistView> list(UUID ownerId);
    Flux<MusicPlaylistEntryView> entries(UUID ownerId, UUID playlistId);
    Mono<MusicPlaylistEntryView> add(UUID ownerId, UUID playlistId, AddMusicPlaylistEntryRequest request);
    Mono<Void> remove(UUID ownerId, UUID entryId);
}
