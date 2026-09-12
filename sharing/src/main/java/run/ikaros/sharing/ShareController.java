package run.ikaros.sharing;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/shares")
public class ShareController {
  private final ShareService service;

  public ShareController(ShareService service) { this.service = service; }

  @PostMapping
  public Mono<ShareView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,
                                @Valid @RequestBody CreateShareRequest request) {
    return service.create(actor, request);
  }

  @GetMapping
  public Flux<ShareView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actor) {
    return service.list(actor);
  }

  @PatchMapping("/{shareId}/expiration")
  public Mono<ShareView> setExpiration(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,
                                       @PathVariable UUID shareId,
                                       @Valid @RequestBody SetShareExpirationRequest request) {
    return service.setExpiration(actor, shareId, request);
  }

  @PutMapping("/{shareId}/restrictions")
  public Mono<ShareView> configureRestrictions(
      @RequestHeader("X-Ikaros-Actor-Id") UUID actor,
      @PathVariable UUID shareId,
      @Valid @RequestBody ConfigureShareRestrictionsRequest request) {
    return service.configureRestrictions(actor, shareId, request);
  }

  @PostMapping("/{shareId}/actions/revoke")
  public Mono<ShareView> revoke(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,
                                @PathVariable UUID shareId) {
    return service.revoke(actor, shareId);
  }

  @PostMapping("/redeem")
  public Mono<ShareView> redeem(@RequestParam String token,
                                @RequestParam(required = false) String password) {
    return service.redeem(token, password);
  }
}
