package run.ikaros.notes;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class PrivateAttachmentService {
  private final PrivateNoteRepository notes;
  private final PrivateAttachmentRepository attachments;
  private final PrivateVaultRepository vaults;

  public PrivateAttachmentService(PrivateNoteRepository n, PrivateAttachmentRepository a, PrivateVaultRepository v) { notes = n; attachments = a; vaults = v; }
  public Mono<PrivateAttachmentView> attach(UUID actorId, UUID noteId, AttachPrivateFileRequest q) {
    return note(actorId, noteId).flatMap(n -> unlocked(actorId, n.vaultId()).then(attachments.save(new PrivateAttachmentEntity(null, noteId, n.vaultId(), actorId, q.attachmentId(), q.encryptedFileName(), Instant.now())))).map(this::view);
  }
  public Flux<PrivateAttachmentView> list(UUID actorId, UUID noteId) { return note(actorId, noteId).flatMapMany(n -> unlocked(actorId, n.vaultId()).flatMapMany(v -> attachments.findAllByNoteIdAndOwnerIdOrderByCreatedAtAsc(noteId, actorId).take(100).map(this::view))); }
  private Mono<PrivateNoteEntity> note(UUID actorId, UUID id) { return notes.findById(id).filter(n -> n.ownerId().equals(actorId) && !n.tombstone()).switchIfEmpty(Mono.error(new NotFoundException("Private Note 不存在或无权访问"))); }
  private Mono<PrivateVaultEntity> unlocked(UUID actorId, UUID id) { return vaults.findById(id).filter(v -> v.ownerId().equals(actorId)).switchIfEmpty(Mono.error(new NotFoundException("Private Vault 不存在或无权访问"))).flatMap(v -> v.status() == VaultStatus.UNLOCKED ? Mono.just(v) : Mono.error(new ConflictException("Private Vault 尚未解锁"))); }
  private PrivateAttachmentView view(PrivateAttachmentEntity x) { return new PrivateAttachmentView(x.id(), x.noteId(), x.attachmentId(), x.encryptedFileName(), x.createdAt()); }
}
