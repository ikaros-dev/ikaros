package run.ikaros.finance;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class LedgerMemberService {
  private final LedgerRepository ledgers;
  private final LedgerMemberRepository members;
  public LedgerMemberService(LedgerRepository ledgers, LedgerMemberRepository members) { this.ledgers = ledgers; this.members = members; }
  public Mono<LedgerMemberView> add(UUID actor, UUID ledgerId, AddLedgerMemberRequest request) { return owner(actor, ledgerId).flatMap(ledger -> members.findByLedgerIdAndPrincipalId(ledgerId, request.principalId()).flatMap(old -> members.save(new LedgerMemberEntity(old.id(), ledgerId, request.principalId(), actor, request.role(), old.createdAt(), old.version()))).switchIfEmpty(members.save(new LedgerMemberEntity(null, ledgerId, request.principalId(), actor, request.role(), Instant.now(), null)))).map(this::view); }
  public Flux<LedgerMemberView> list(UUID actor, UUID ledgerId) { return owner(actor, ledgerId).flatMapMany(ledger -> members.findAllByLedgerIdOrderByCreatedAtAsc(ledgerId).take(100)).map(this::view); }
  public Mono<Void> remove(UUID actor, UUID ledgerId, UUID principalId) { return owner(actor, ledgerId).flatMap(ledger -> { if (ledger.ownerId().equals(principalId)) return Mono.error(new ConflictException("Ledger Owner 不能被移除")); return members.findByLedgerIdAndPrincipalId(ledgerId, principalId).switchIfEmpty(Mono.error(new NotFoundException("Ledger Member 不存在"))).flatMap(members::delete); }).then(); }
  private Mono<LedgerEntity> owner(UUID actor, UUID ledgerId) { return ledgers.findById(ledgerId).filter(ledger -> ledger.ownerId().equals(actor)).switchIfEmpty(Mono.error(new NotFoundException("Ledger 不存在或无权管理成员"))); }
  private LedgerMemberView view(LedgerMemberEntity member) { return new LedgerMemberView(member.id(), member.ledgerId(), member.principalId(), member.role(), member.createdAt()); }
}
