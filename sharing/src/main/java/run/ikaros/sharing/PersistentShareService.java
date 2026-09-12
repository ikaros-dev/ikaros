package run.ikaros.sharing;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
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

  @Override
  public Mono<ShareView> create(UUID issuer, CreateShareRequest request) {
    if (issuer == null) return Mono.error(new IllegalArgumentException("分享发起者不能为空"));
    if (request == null || request.targetType() == null || request.targetId() == null
        || request.granteeType() == null || request.capabilities() == null
        || request.capabilities().isBlank()) {
      return Mono.error(new IllegalArgumentException("分享目标、授予方式和能力不能为空"));
    }
    String targetType = request.targetType().trim().toUpperCase(Locale.ROOT);
    Mono<Void> ownership;
    if ("RESOURCE".equals(targetType)) ownership = resources.requireOwned(issuer, request.targetId());
    else if ("COLLECTION".equals(targetType)) ownership = collections.requireOwned(issuer, request.targetId());
    else return Mono.error(new IllegalArgumentException("不支持的分享目标类型"));

    try {
      validateRestrictions(request.password(), request.maxAccessCount());
    } catch (IllegalArgumentException error) {
      return Mono.error(error);
    }
    if (request.expiresAt() != null && !request.expiresAt().isAfter(Instant.now())) {
      return Mono.error(new IllegalArgumentException("Share 过期时间必须在未来"));
    }
    if (request.granteeType() == ShareGranteeType.USER && request.granteeId() == null) {
      return Mono.error(new IllegalArgumentException("用户分享必须指定 granteeId"));
    }
    if (request.granteeType() != ShareGranteeType.USER && request.granteeId() != null) {
      return Mono.error(new IllegalArgumentException("当前授予方式不支持 granteeId"));
    }

    String token = request.granteeType() == ShareGranteeType.LINK_TOKEN
        ? UUID.randomUUID().toString() : null;
    Instant now = Instant.now();
    String password = blankToNull(request.password());
    ShareEntity entity = new ShareEntity(null, issuer, targetType, request.targetId(),
        request.granteeType(), request.granteeId(), request.capabilities().trim(),
        token == null ? null : digest(token), request.expiresAt(),
        password == null ? null : digest(password), Boolean.TRUE.equals(request.allowDownload()),
        request.maxAccessCount(), 0, ShareStatus.ACTIVE, now, now, null);
    return ownership.then(Mono.defer(() -> repository.save(entity)))
        .map(share -> withToken(view(share), token));
  }

  @Override
  public Flux<ShareView> list(UUID issuer) {
    return repository.findAllByIssuerIdOrderByCreatedAtDesc(issuer).take(100).map(this::view);
  }

  @Override
  public Mono<ShareView> setExpiration(UUID issuer, UUID shareId,
                                        SetShareExpirationRequest request) {
    if (request == null || request.expiresAt() == null) {
      return Mono.error(new IllegalArgumentException("Share 过期时间不能为空"));
    }
    if (!request.expiresAt().isAfter(Instant.now())) {
      return Mono.error(new IllegalArgumentException("Share 过期时间必须在未来"));
    }
    return repository.findById(shareId)
        .filter(share -> share.issuerId().equals(issuer))
        .switchIfEmpty(Mono.error(new NotFoundException("Share 不存在或无权操作")))
        .flatMap(share -> {
          if (share.status() != ShareStatus.ACTIVE) {
            return Mono.error(new IllegalStateException("当前 Share 不可设置有效期"));
          }
          return repository.save(copy(share, share.status(), request.expiresAt(),
              share.passwordDigest(), share.allowDownload(), share.maxAccessCount(),
              safeCount(share.accessCount())));
        })
        .map(this::view);
  }

  @Override
  public Mono<ShareView> configureRestrictions(UUID issuer, UUID shareId,
                                                ConfigureShareRestrictionsRequest request) {
    if (request == null) return Mono.error(new IllegalArgumentException("访问限制不能为空"));
    try {
      validateRestrictions(request.password(), request.maxAccessCount());
    } catch (IllegalArgumentException error) {
      return Mono.error(error);
    }
    String password = blankToNull(request.password());
    return repository.findById(shareId)
        .filter(share -> share.issuerId().equals(issuer))
        .switchIfEmpty(Mono.error(new NotFoundException("Share 不存在或无权操作")))
        .flatMap(share -> {
          if (share.status() != ShareStatus.ACTIVE) {
            return Mono.error(new IllegalStateException("当前 Share 不可配置访问限制"));
          }
          return repository.save(copy(share, share.status(), share.expiresAt(),
              password == null ? null : digest(password),
              Boolean.TRUE.equals(request.allowDownload()), request.maxAccessCount(),
              safeCount(share.accessCount())));
        })
        .map(this::view);
  }

  @Override
  public Mono<ShareView> revoke(UUID issuer, UUID shareId) {
    return repository.findById(shareId)
        .filter(share -> share.issuerId().equals(issuer))
        .switchIfEmpty(Mono.error(new NotFoundException("Share 不存在或无权操作")))
        .flatMap(share -> {
          if (share.status() != ShareStatus.ACTIVE) {
            return Mono.error(new ConflictException("share.not_active", "当前 Share 已不可撤销"));
          }
          return repository.save(copy(share, ShareStatus.REVOKED, share.expiresAt(),
              share.passwordDigest(), share.allowDownload(), share.maxAccessCount(),
              safeCount(share.accessCount())));
        })
        .map(this::view);
  }

  @Override
  public Mono<ShareView> redeem(String token, String password) {
    if (token == null || token.isBlank()) {
      return Mono.error(new ShareAccessException(ShareAccessFailureReason.MISSING_TOKEN,
          "请输入分享令牌"));
    }
    return repository.findByTokenDigest(digest(token.trim()))
        .switchIfEmpty(Mono.error(new ShareAccessException(
            ShareAccessFailureReason.INVALID_TOKEN, "分享链接无效或已失效")))
        .flatMap(share -> {
          if (share.status() == ShareStatus.REVOKED) {
            return Mono.error(new ShareAccessException(ShareAccessFailureReason.REVOKED,
                "分享链接已撤销"));
          }
          if (share.expiresAt() != null && !share.expiresAt().isAfter(Instant.now())) {
            return Mono.error(new ShareAccessException(ShareAccessFailureReason.EXPIRED,
                "分享链接已过期"));
          }
          if (share.status() != ShareStatus.ACTIVE) {
            return Mono.error(new ShareAccessException(ShareAccessFailureReason.INVALID_TOKEN,
                "分享链接不可用"));
          }
          if (share.passwordDigest() != null && !MessageDigest.isEqual(
              share.passwordDigest().getBytes(StandardCharsets.UTF_8),
              digest(blankToNull(password) == null ? "" : password.trim())
                  .getBytes(StandardCharsets.UTF_8))) {
            return Mono.error(new IllegalArgumentException("Share 密码错误"));
          }
          int count = safeCount(share.accessCount());
          if (share.maxAccessCount() != null && count >= share.maxAccessCount()) {
            return Mono.error(new IllegalStateException("Share 已达到最大访问次数"));
          }
          if (share.maxAccessCount() == null) return Mono.just(view(share));
          return repository.save(copy(share, share.status(), share.expiresAt(),
              share.passwordDigest(), share.allowDownload(), share.maxAccessCount(), count + 1))
              .map(this::view);
        });
  }

  private ShareEntity copy(ShareEntity share, ShareStatus status, Instant expiresAt,
                           String passwordDigest, Boolean allowDownload,
                           Integer maxAccessCount, int accessCount) {
    return new ShareEntity(share.id(), share.issuerId(), share.targetType(), share.targetId(),
        share.granteeType(), share.granteeId(), share.capabilities(), share.tokenDigest(),
        expiresAt, passwordDigest, Boolean.TRUE.equals(allowDownload), maxAccessCount,
        accessCount, status, share.createdAt(), Instant.now(), share.version());
  }

  private ShareView view(ShareEntity share) {
    return new ShareView(share.id(), share.issuerId(), share.targetType(), share.targetId(),
        share.granteeType(), share.granteeId(), share.capabilities(), share.expiresAt(),
        share.status(), share.createdAt(), null,
        new ShareRestrictionView(share.passwordDigest() != null,
            Boolean.TRUE.equals(share.allowDownload()), share.maxAccessCount(),
            safeCount(share.accessCount())));
  }

  private ShareView withToken(ShareView share, String token) {
    return new ShareView(share.id(), share.issuerId(), share.targetType(), share.targetId(),
        share.granteeType(), share.granteeId(), share.capabilities(), share.expiresAt(),
        share.status(), share.createdAt(), token, share.restrictions());
  }

  private void validateRestrictions(String password, Integer maxAccessCount) {
    if (password != null && password.isBlank()) {
      throw new IllegalArgumentException("Share 密码不能为空");
    }
    if (maxAccessCount != null && maxAccessCount < 1) {
      throw new IllegalArgumentException("Share 最大访问次数必须大于 0");
    }
  }

  private String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value;
  }

  private int safeCount(Integer value) { return value == null ? 0 : value; }

  private String digest(String value) {
    try {
      return Base64.getUrlEncoder().withoutPadding().encodeToString(
          MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException error) {
      throw new IllegalStateException(error);
    }
  }
}
