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
import run.ikaros.resource.api.CollectionOwnershipQuery;
import run.ikaros.resource.api.ResourceOwnershipQuery;

@Service
public class PersistentShareService implements ShareService {
  private final ShareRepository repository;
  private final ResourceOwnershipQuery resources;
  private final CollectionOwnershipQuery collections;

  public PersistentShareService(ShareRepository repository,
                                ResourceOwnershipQuery resources,
                                CollectionOwnershipQuery collections) {
    this.repository = repository;
    this.resources = resources;
    this.collections = collections;
  }
  public Mono<ShareView> create(UUID issuer, CreateShareRequest request) {
    if (issuer == null) return Mono.error(new IllegalArgumentException("分享发起者不能为空"));
    if (request == null || request.targetType() == null || request.targetId() == null
        || request.granteeType() == null || request.capabilities() == null
        || request.capabilities().isBlank()) {
      return Mono.error(new IllegalArgumentException("分享目标、授予方式和能力不能为空"));
    }
    String targetType = request.targetType().trim().toUpperCase(java.util.Locale.ROOT);
    Mono<Void> ownership;
    if ("RESOURCE".equals(targetType)) {
      ownership = resources.requireOwned(issuer, request.targetId());
    } else if ("COLLECTION".equals(targetType)) {
      ownership = collections.requireOwned(issuer, request.targetId());
    } else {
      return Mono.error(new IllegalArgumentException("不支持的分享目标类型"));
    }
    if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now())) return Mono.error(new IllegalArgumentException("Share 过期时间必须在未来"));
    if (request.granteeType() == ShareGranteeType.USER && request.granteeId() == null) return Mono.error(new IllegalArgumentException("用户分享必须指定 granteeId"));
    if (request.granteeType() != ShareGranteeType.USER && request.granteeId() != null) return Mono.error(new IllegalArgumentException("当前授予方式不支持 granteeId"));
    String token = request.granteeType() == ShareGranteeType.LINK_TOKEN ? UUID.randomUUID().toString() : null;
    Instant now = Instant.now();
    ShareEntity entity = new ShareEntity(null, issuer, targetType, request.targetId(), request.granteeType(), request.granteeId(), request.capabilities().trim(), token == null ? null : digest(token), request.expiresAt(), ShareStatus.ACTIVE, now, now, null);
    return ownership.then(Mono.defer(() -> repository.save(entity))).map(share -> withToken(view(share), token));
  }
  public Flux<ShareView> list(UUID issuer) { return repository.findAllByIssuerIdOrderByCreatedAtDesc(issuer).take(100).map(this::view); }
  public Mono<ShareView> revoke(UUID issuer, UUID id) { return repository.findById(id).filter(share -> share.issuerId().equals(issuer)).switchIfEmpty(Mono.error(new NotFoundException("Share 不存在或无权操作"))).flatMap(share -> repository.save(new ShareEntity(share.id(), share.issuerId(), share.targetType(), share.targetId(), share.granteeType(), share.granteeId(), share.capabilities(), share.tokenDigest(), share.expiresAt(), ShareStatus.REVOKED, share.createdAt(), Instant.now(), share.version()))).map(this::view); }
  public Mono<ShareView> redeem(String token) { if (token == null || token.isBlank()) return Mono.error(new IllegalArgumentException("Share Token 不能为空")); return repository.findByTokenDigest(digest(token)).switchIfEmpty(Mono.error(new NotFoundException("Share Token 无效"))).flatMap(share -> { if (share.status() != ShareStatus.ACTIVE) return Mono.error(new NotFoundException("Share 已撤销")); if (share.expiresAt() != null && !share.expiresAt().isAfter(Instant.now())) return Mono.error(new NotFoundException("Share 已过期")); return Mono.just(view(share)); }); }
  private String digest(String token) { try { return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); } }
  private ShareView view(ShareEntity share) { return new ShareView(share.id(), share.issuerId(), share.targetType(), share.targetId(), share.granteeType(), share.granteeId(), share.capabilities(), share.expiresAt(), share.status(), share.createdAt(), null); }
  private ShareView withToken(ShareView share, String token) { return new ShareView(share.id(), share.issuerId(), share.targetType(), share.targetId(), share.granteeType(), share.granteeId(), share.capabilities(), share.expiresAt(), share.status(), share.createdAt(), token); }
}
