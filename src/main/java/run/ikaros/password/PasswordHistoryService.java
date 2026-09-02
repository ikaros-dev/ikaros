package run.ikaros.password;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PasswordHistoryService {
  private final PasswordVaultItemRepository items;
  private final PasswordHistoryRepository history;
  private final PasswordVaultRepository vaults;

  public PasswordHistoryService(PasswordVaultItemRepository i, PasswordHistoryRepository h, PasswordVaultRepository v) {
    items = i;
    history = h;
    vaults = v;
  }

  public Mono<PasswordHistoryView> record(UUID actorId, UUID itemId, RecordPasswordHistoryRequest q) {
    return item(actorId, itemId)
        .flatMap(i -> unlocked(actorId, i.vaultId())
            .then(history.save(new PasswordHistoryEntity(null, itemId, i.vaultId(), actorId, q.revision(), q.encryptedPayload(), Instant.now())))
            .map(this::view));
  }

  public Flux<PasswordHistoryView> list(UUID actorId, UUID itemId) {
    return item(actorId, itemId)
        .flatMapMany(i -> unlocked(actorId, i.vaultId())
            .flatMapMany(v -> history.findAllByItemIdAndOwnerIdOrderByRevisionDesc(itemId, actorId).map(this::view)));
  }

  private Mono<PasswordVaultItemEntity> item(UUID actorId, UUID itemId) {
    return items.findById(itemId).filter(i -> i.ownerId().equals(actorId) && !i.tombstone())
        .switchIfEmpty(Mono.error(new NotFoundException("Vault Item 不存在或无权访问")));
  }

  private Mono<PasswordVaultEntity> unlocked(UUID actorId, UUID vaultId) {
    return vaults.findById(vaultId).filter(v -> v.ownerId().equals(actorId))
        .switchIfEmpty(Mono.error(new NotFoundException("Password Vault 不存在或无权访问")))
        .flatMap(v -> v.status() == PasswordVaultStatus.UNLOCKED ? Mono.just(v) : Mono.error(new ConflictException("Password Vault 尚未解锁")));
  }

  private PasswordHistoryView view(PasswordHistoryEntity h) { return new PasswordHistoryView(h.id(), h.itemId(), h.revision(), h.encryptedPayload(), h.createdAt()); }
}
