package run.ikaros.password;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PasswordSyncService {
  private final PasswordVaultRepository vaults;
  private final PasswordVaultItemRepository items;

  public PasswordSyncService(PasswordVaultRepository v, PasswordVaultItemRepository i) { vaults = v; items = i; }

  public Mono<PasswordSyncView> sync(UUID actorId, UUID vaultId, Instant cursor) {
    return unlocked(actorId, vaultId)
        .flatMap(v -> items.findAllByVaultIdAndOwnerIdAndUpdatedAtGreaterThanOrderByUpdatedAtAsc(vaultId, actorId, cursor == null ? Instant.EPOCH : cursor).collectList())
        .map(list -> {
          Instant next = list.isEmpty() ? Instant.now() : list.get(list.size() - 1).updatedAt();
          return new PasswordSyncView(list.stream().map(i -> new PasswordVaultItemView(i.id(), i.vaultId(), i.itemType(), i.encryptedName(), i.encryptedPayload(), i.encryptedCustomFields(), i.favorite(), i.revision(), i.tombstone(), i.createdAt(), i.updatedAt())).toList(), next);
        });
  }

  private Mono<PasswordVaultEntity> unlocked(UUID actorId, UUID vaultId) {
    return vaults.findById(vaultId).filter(v -> v.ownerId().equals(actorId))
        .switchIfEmpty(Mono.error(new NotFoundException("Password Vault 不存在或无权访问")))
        .flatMap(v -> v.status() == PasswordVaultStatus.UNLOCKED ? Mono.just(v) : Mono.error(new ConflictException("Password Vault 尚未解锁")));
  }
}
