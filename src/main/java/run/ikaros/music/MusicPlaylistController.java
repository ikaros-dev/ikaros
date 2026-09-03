package run.ikaros.music;
import jakarta.validation.Valid; import java.util.UUID; import org.springframework.web.bind.annotation.*; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
@RestController @RequestMapping("/api/music/playlists") public class MusicPlaylistController {
    private final MusicPlaylistService service; public MusicPlaylistController(MusicPlaylistService service){this.service=service;}
    @PostMapping public Mono<MusicPlaylistView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@Valid @RequestBody CreateMusicPlaylistRequest r){return service.create(owner,r);}
    @GetMapping public Flux<MusicPlaylistView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID owner){return service.list(owner);}
    @GetMapping("/{playlistId}/entries") public Flux<MusicPlaylistEntryView> entries(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID playlistId){return service.entries(owner,playlistId);}
    @PostMapping("/{playlistId}/entries") public Mono<MusicPlaylistEntryView> add(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID playlistId,@Valid @RequestBody AddMusicPlaylistEntryRequest r){return service.add(owner,playlistId,r);}
    @DeleteMapping("/entries/{entryId}") public Mono<Void> remove(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID entryId){return service.remove(owner,entryId);}
}
