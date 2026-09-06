package run.ikaros.finance;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class BalanceSnapshotService {
  private final AccountRepository accounts;
  private final BalanceSnapshotRepository snapshots;
  private final FinanceLedgerAccessService access;

  public BalanceSnapshotService(AccountRepository a, BalanceSnapshotRepository s, FinanceLedgerAccessService x) {
    accounts = a;
    snapshots = s;
    access = x;
  }

  public Mono<BalanceSnapshotView> create(UUID actor, UUID ledgerId, CreateBalanceSnapshotRequest q) {
    return access.requireEditor(actor, ledgerId)
        .then(accounts.findById(q.accountId())
            .filter(account -> account.ledgerId().equals(ledgerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Account 不存在或不属于该 Ledger")))
            .flatMap(account -> snapshots.findByAccountIdAndSnapshotDate(q.accountId(), q.snapshotDate())
                .flatMap(existing -> Mono.<BalanceSnapshotEntity>error(new ConflictException("该账户该日期已存在快照")))
                .switchIfEmpty(snapshots.save(new BalanceSnapshotEntity(null, ledgerId, q.accountId(), account.ownerId(),
                    q.snapshotDate(), account.currentBalance(), account.currency(), Instant.now(), null)))))
        .map(this::view);
  }

  public Flux<BalanceSnapshotView> list(UUID actor, UUID ledgerId) {
    return access.access(actor, ledgerId)
        .flatMapMany(role -> snapshots.findAllByLedgerIdOrderBySnapshotDateDesc(ledgerId).take(100).map(this::view));
  }

  private BalanceSnapshotView view(BalanceSnapshotEntity x) {
    return new BalanceSnapshotView(x.id(), x.accountId(), x.snapshotDate(), x.balance(), x.currency(), x.createdAt());
  }
}
