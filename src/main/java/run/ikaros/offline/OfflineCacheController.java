package run.ikaros.offline;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
@RestController
@RequestMapping("/api/offline/cache")
public class OfflineCacheController {
    private final OfflineCacheService service;
    public OfflineCacheController(OfflineCacheService service){this.service=service;}
    @PostMapping public Mono<CacheEntryView> put(@RequestHeader("X-Ikaros-Actor-Id") UUID user,@Valid @RequestBody CreateCacheEntryRequest request){return service.put(user,request);}
    @GetMapping public Flux<CacheEntryView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID user,@RequestParam UUID deviceId){return service.list(user,deviceId);}
    @PostMapping("/{entryId}/touch") public Mono<CacheEntryView> touch(@RequestHeader("X-Ikaros-Actor-Id") UUID user,@PathVariable UUID entryId){return service.touch(user,entryId);}
    @DeleteMapping("/{entryId}") public Mono<ResponseEntity<Void>> evict(@RequestHeader("X-Ikaros-Actor-Id") UUID user,@PathVariable UUID entryId){return service.evict(user,entryId).thenReturn(ResponseEntity.noContent().build());}
}
