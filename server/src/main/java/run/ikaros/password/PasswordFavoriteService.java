package run.ikaros.password;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PasswordFavoriteService {
  private final PasswordVaultRepository vaults;
  private final PasswordVaultItemRepository items;

  public PasswordFavoriteService(PasswordVaultRepository v, PasswordVaultItemRepository i) {
    vaults = v;
    items = i;
  }

  public Flux<PasswordVaultItemView> list(UUID actorId, UUID vaultId) {
    return unlocked(actorId, vaultId)
        .flatMapMany(v -> items.findAllByVaultIdAndOwnerIdAndFavoriteTrueAndTombstoneFalseOrderByUpdatedAtDesc(vaultId, actorId)
            .take(100).map(i -> new PasswordVaultItemView(i.id(), i.vaultId(), i.itemType(), i.encryptedName(), i.encryptedPayload(), i.encryptedCustomFields(), i.favorite(), i.revision(), i.tombstone(), i.createdAt(), i.updatedAt())));
  }

  private Mono<PasswordVaultEntity> unlocked(UUID actorId, UUID vaultId) {
    return vaults.findById(vaultId)
        .filter(v -> v.ownerId().equals(actorId))
        .switchIfEmpty(Mono.error(new NotFoundException("Password Vault 不存在或无权访问")))
        .flatMap(v -> v.status() == PasswordVaultStatus.UNLOCKED
            ? Mono.just(v)
            : Mono.error(new ConflictException("Password Vault 尚未解锁")));
  }
}
