package run.ikaros.password;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PasswordFolderService {
  private final PasswordVaultRepository vaults;
  private final PasswordFolderRepository folders;
  private final PasswordVaultItemRepository items;
  private final PasswordItemFolderRepository bindings;

  public PasswordFolderService(PasswordVaultRepository v, PasswordFolderRepository f, PasswordVaultItemRepository i, PasswordItemFolderRepository b) {
    vaults = v; folders = f; items = i; bindings = b;
  }

  public Mono<PasswordFolderView> create(UUID actorId, UUID vaultId, CreatePasswordFolderRequest q) {
    return unlocked(actorId, vaultId).flatMap(x -> folders.save(new PasswordFolderEntity(null, vaultId, actorId, q.parentId(), q.encryptedName(), Instant.now(), null))).map(this::view);
  }

  public Flux<PasswordFolderView> list(UUID actorId, UUID vaultId) {
    return unlocked(actorId, vaultId).flatMapMany(x -> folders.findAllByVaultIdAndOwnerIdOrderByCreatedAtAsc(vaultId, actorId).map(this::view));
  }

  public Mono<PasswordFolderView> bind(UUID actorId, UUID itemId, BindPasswordFolderRequest q) {
    return items.findById(itemId).filter(i -> i.ownerId().equals(actorId) && !i.tombstone())
        .switchIfEmpty(Mono.error(new NotFoundException("Vault Item 不存在或无权访问")))
        .flatMap(i -> unlocked(actorId, i.vaultId())
            .then(folders.findById(q.folderId()).filter(f -> f.ownerId().equals(actorId) && f.vaultId().equals(i.vaultId()))
                .switchIfEmpty(Mono.error(new NotFoundException("Folder 不存在或不属于该 Vault")))
                .flatMap(f -> bindings.findByItemIdAndOwnerId(itemId, actorId)
                    .flatMap(old -> bindings.save(new PasswordItemFolderEntity(old.id(), itemId, f.id(), i.vaultId(), actorId, old.createdAt())))
                    .switchIfEmpty(bindings.save(new PasswordItemFolderEntity(null, itemId, f.id(), i.vaultId(), actorId, Instant.now())))
                    .thenReturn(view(f)))));
  }

  private Mono<PasswordVaultEntity> unlocked(UUID actorId, UUID vaultId) {
    return vaults.findById(vaultId).filter(v -> v.ownerId().equals(actorId))
        .switchIfEmpty(Mono.error(new NotFoundException("Password Vault 不存在或无权访问")))
        .flatMap(v -> v.status() == PasswordVaultStatus.UNLOCKED ? Mono.just(v) : Mono.error(new ConflictException("Password Vault 尚未解锁")));
  }

  private PasswordFolderView view(PasswordFolderEntity f) { return new PasswordFolderView(f.id(), f.vaultId(), f.parentId(), f.encryptedName(), f.createdAt()); }
}
