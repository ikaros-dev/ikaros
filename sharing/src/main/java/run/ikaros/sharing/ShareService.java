package run.ikaros.sharing;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ShareService {
  Mono<ShareView> create(UUID issuer, CreateShareRequest request);
  Flux<ShareView> list(UUID issuer);
  Mono<ShareView> setExpiration(UUID issuer, UUID shareId, SetShareExpirationRequest request);
  Mono<ShareView> revoke(UUID issuer, UUID shareId);
  Mono<ShareView> configureRestrictions(UUID issuer, UUID shareId,
                                         ConfigureShareRestrictionsRequest request);
  default Mono<ShareView> redeem(String token) { return redeem(token, null); }
  Mono<ShareView> redeem(String token, String password);
}
