package run.ikaros.photo;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.IfMatchVersion;
import run.ikaros.operations.api.TaskReference;

@RestController
@RequestMapping("/api/photos")
public class PhotoController {
    private final PhotoService service;

    public PhotoController(PhotoService s) { service = s; }
    @PostMapping public Mono<PhotoView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @Valid @RequestBody CreatePhotoRequest r) { return service.create(o, r); }
    @GetMapping("/timeline") public Flux<PhotoView> timeline(@RequestHeader("X-Ikaros-Actor-Id") UUID o) { return service.timeline(o); }
    @GetMapping("/timeline/by-day") public Flux<PhotoTimelineGroupView> timelineByDay(@RequestHeader("X-Ikaros-Actor-Id") UUID o) { return service.timelineByDay(o); }
    @GetMapping("/{photoId}/assets") public Flux<PhotoAssetView> assets(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @PathVariable UUID photoId) { return service.assets(o, photoId); }
    @GetMapping("/{photoId}/thumbnail/status") public Mono<PhotoThumbnailStatusView> thumbnailStatus(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @PathVariable UUID photoId) { return service.thumbnailStatus(o, photoId); }
    @PostMapping("/{photoId}/actions/generate-thumbnail") public Mono<TaskReference> thumbnail(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @PathVariable UUID photoId) { return service.requestThumbnail(o, photoId); }
    @PostMapping("/{photoId}/actions/regenerate-thumbnail") public Mono<TaskReference> regenerateThumbnail(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @PathVariable UUID photoId) { return service.regenerateThumbnail(o, photoId); }
    @PostMapping("/{photoId}/actions/set-primary") public Mono<PhotoAssetView> primary(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @PathVariable UUID photoId, @Valid @RequestBody SetPrimaryPhotoAssetRequest r) { return service.setPrimary(o, photoId, r); }
    @PostMapping("/albums") public Mono<PhotoAlbumView> album(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @Valid @RequestBody CreatePhotoAlbumRequest r) { return service.createAlbum(o, r); }
    @GetMapping("/albums") public Flux<PhotoAlbumView> albums(@RequestHeader("X-Ikaros-Actor-Id") UUID o) { return service.albums(o); }
    @PatchMapping("/albums/{albumId}") public Mono<ResponseEntity<PhotoAlbumView>> updateAlbum(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @PathVariable UUID albumId, @RequestHeader(value = "If-Match", required = false) String ifMatch, @Valid @RequestBody UpdatePhotoAlbumRequest r) { return service.updateAlbum(o, albumId, r, IfMatchVersion.parse(ifMatch)).map(view -> ResponseEntity.ok().eTag(IfMatchVersion.etag(view.version())).body(view)); }
    @GetMapping("/albums/{albumId}/photos") public Flux<PhotoView> albumPhotos(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @PathVariable UUID albumId) { return service.albumPhotos(o, albumId); }
    @PostMapping("/albums/{albumId}/photos") public Mono<Void> add(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @PathVariable UUID albumId, @Valid @RequestBody AddPhotoAlbumMemberRequest r) { return service.addToAlbum(o, albumId, r); }
    @DeleteMapping("/albums/{albumId}/photos/{photoId}") public Mono<Void> remove(@RequestHeader("X-Ikaros-Actor-Id") UUID o, @PathVariable UUID albumId, @PathVariable UUID photoId) { return service.removeFromAlbum(o, albumId, photoId); }
}
