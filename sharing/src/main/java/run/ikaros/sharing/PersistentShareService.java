package run.ikaros.sharing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentShareService implements ShareService {
  private final ShareRepository repository;
  public PersistentShareService(ShareRepository repository) { this.repository = repository; }
  public Mono<ShareView> create(UUID issuer, CreateShareRequest request) {
    if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now())) return Mono.error(new IllegalArgumentException("Share 过期时间必须在未来"));
    if (request.granteeType() == ShareGranteeType.USER && request.granteeId() == null) return Mono.error(new IllegalArgumentException("用户分享必须指定 granteeId"));
    String token = request.granteeType() == ShareGranteeType.LINK_TOKEN ? UUID.randomUUID().toString() : null;
    Instant now = Instant.now();
    return repository.save(new ShareEntity(null, issuer, request.targetType(), request.targetId(), request.granteeType(), request.granteeId(), request.capabilities(), token == null ? null : digest(token), request.expiresAt(), ShareStatus.ACTIVE, now, now, null)).map(share -> withToken(view(share), token));
  }
  public Flux<ShareView> list(UUID issuer) { return repository.findAllByIssuerIdOrderByCreatedAtDesc(issuer).take(100).map(this::view); }
  public Mono<ShareView> revoke(UUID issuer, UUID id) { return repository.findById(id).filter(share -> share.issuerId().equals(issuer)).switchIfEmpty(Mono.error(new NotFoundException("Share 不存在或无权操作"))).flatMap(share -> repository.save(new ShareEntity(share.id(), share.issuerId(), share.targetType(), share.targetId(), share.granteeType(), share.granteeId(), share.capabilities(), share.tokenDigest(), share.expiresAt(), ShareStatus.REVOKED, share.createdAt(), Instant.now(), share.version()))).map(this::view); }
  public Mono<ShareView> redeem(String token) { if (token == null || token.isBlank()) return Mono.error(new IllegalArgumentException("Share Token 不能为空")); return repository.findByTokenDigest(digest(token)).switchIfEmpty(Mono.error(new NotFoundException("Share Token 无效"))).flatMap(share -> { if (share.status() != ShareStatus.ACTIVE) return Mono.error(new NotFoundException("Share 已撤销")); if (share.expiresAt() != null && !share.expiresAt().isAfter(Instant.now())) return Mono.error(new NotFoundException("Share 已过期")); return Mono.just(view(share)); }); }
  private String digest(String token) { try { return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
  private ShareView view(ShareEntity share) { return new ShareView(share.id(), share.issuerId(), share.targetType(), share.targetId(), share.granteeType(), share.granteeId(), share.capabilities(), share.expiresAt(), share.status(), share.createdAt(), null); }
  private ShareView withToken(ShareView share, String token) { return new ShareView(share.id(), share.issuerId(), share.targetType(), share.targetId(), share.granteeType(), share.granteeId(), share.capabilities(), share.expiresAt(), share.status(), share.createdAt(), token); }
}
