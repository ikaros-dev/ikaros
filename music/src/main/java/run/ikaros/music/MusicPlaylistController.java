package run.ikaros.music;
import jakarta.validation.Valid; import java.util.UUID; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono; import run.ikaros.common.IfMatchVersion;
@RestController @RequestMapping("/api/music/playlists") public class MusicPlaylistController {
    private final MusicPlaylistService service; public MusicPlaylistController(MusicPlaylistService service){this.service=service;}
    @PostMapping public Mono<MusicPlaylistView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@Valid @RequestBody CreateMusicPlaylistRequest r){return service.create(owner,r);}
    @PatchMapping("/{playlistId}") public Mono<ResponseEntity<MusicPlaylistView>> update(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID playlistId,@RequestHeader(value="If-Match",required=false) String ifMatch,@Valid @RequestBody UpdateMusicPlaylistRequest r){return service.update(owner,playlistId,new UpdateMusicPlaylistRequest(r.name(),r.description(),IfMatchVersion.parse(ifMatch))).map(view->ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view));}
    @GetMapping public Flux<MusicPlaylistView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID owner){return service.list(owner);}
    @GetMapping("/{playlistId}/entries") public Flux<MusicPlaylistEntryView> entries(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID playlistId){return service.entries(owner,playlistId);}
    @PostMapping("/{playlistId}/entries") public Mono<MusicPlaylistEntryView> add(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID playlistId,@Valid @RequestBody AddMusicPlaylistEntryRequest r){return service.add(owner,playlistId,r);}
    @PatchMapping("/{playlistId}/entries/order") public Mono<ResponseEntity<MusicPlaylistView>> reorder(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID playlistId,@RequestHeader("If-Match") String ifMatch,@Valid @RequestBody ReorderMusicPlaylistRequest r){return service.reorder(owner,playlistId,r,IfMatchVersion.parse(ifMatch)).map(view->ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view));}
    @DeleteMapping("/entries/{entryId}") public Mono<Void> remove(@RequestHeader("X-Ikaros-Actor-Id") UUID owner,@PathVariable UUID entryId){return service.remove(owner,entryId);}
}
