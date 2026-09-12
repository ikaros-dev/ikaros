package run.ikaros.sharing;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
public class InviteController {
  private final InviteService service;
  public InviteController(InviteService service) { this.service = service; }

  @PostMapping("/rooms/{roomId}/invites")
  public Mono<InviteView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,
      @RequestHeader(value = "Idempotency-Key", required = false) String headerKey,
      @PathVariable UUID roomId, @Valid @RequestBody CreateInviteRequest request) {
    String key = request.idempotencyKey() == null || request.idempotencyKey().isBlank()
        ? headerKey : request.idempotencyKey();
    return service.create(actor, roomId,
        new CreateInviteRequest(request.inviteeId(), request.role(), key, request.expiresAt()));
  }

  @GetMapping("/invites")
  public Flux<InviteView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actor) { return service.list(actor); }
  @PostMapping("/invites/{id}/actions/accept")
  public Mono<InviteView> accept(@RequestHeader("X-Ikaros-Actor-Id") UUID actor, @PathVariable UUID id) { return service.accept(actor, id); }
  @PostMapping("/invites/{id}/actions/decline")
  public Mono<InviteView> decline(@RequestHeader("X-Ikaros-Actor-Id") UUID actor, @PathVariable UUID id) { return service.decline(actor, id); }
  @PostMapping("/invites/{id}/actions/revoke")
  public Mono<InviteView> revoke(@RequestHeader("X-Ikaros-Actor-Id") UUID actor, @PathVariable UUID id) { return service.revoke(actor, id); }
}
