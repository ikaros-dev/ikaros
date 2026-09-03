package run.ikaros.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class ReconciliationService {
  private final AccountRepository accounts;
  private final ReconciliationRepository reconciliations;
  private final FinanceLedgerAccessService access;

  public ReconciliationService(AccountRepository a, ReconciliationRepository r, FinanceLedgerAccessService x) {
    accounts = a;
    reconciliations = r;
    access = x;
  }

  public Mono<ReconciliationView> create(UUID actor, UUID ledgerId, CreateReconciliationRequest q) {
    return access.requireEditor(actor, ledgerId)
        .then(accounts.findById(q.accountId())
            .filter(account -> account.ledgerId().equals(ledgerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Account 不存在或不属于该 Ledger")))
            .flatMap(account -> {
              BigDecimal difference = q.statementBalance().subtract(account.currentBalance());
              ReconciliationStatus status = difference.signum() == 0
                  ? ReconciliationStatus.MATCHED : ReconciliationStatus.DIFFERENCE;
              Instant now = Instant.now();
              return reconciliations.save(new ReconciliationEntity(null, ledgerId, q.accountId(), account.ownerId(),
                  q.statementDate(), q.statementBalance(), account.currentBalance(), difference, status, q.note(), now, now, null));
            }))
        .map(this::view);
  }

  public Flux<ReconciliationView> list(UUID actor, UUID ledgerId) {
    return access.access(actor, ledgerId)
        .flatMapMany(role -> reconciliations.findAllByLedgerIdOrderByStatementDateDesc(ledgerId).take(100).map(this::view));
  }

  public Mono<ReconciliationView> close(UUID actor, UUID id) {
    return reconciliations.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Reconciliation 不存在或无权访问")))
        .flatMap(reconciliation -> access.requireEditor(actor, reconciliation.ledgerId())
            .then(Mono.defer(() -> {
              if (reconciliation.status() == ReconciliationStatus.OPEN) {
                return Mono.error(new ConflictException("Open 对账不能直接关闭"));
              }
              return reconciliations.save(new ReconciliationEntity(reconciliation.id(), reconciliation.ledgerId(),
                  reconciliation.accountId(), reconciliation.ownerId(), reconciliation.statementDate(),
                  reconciliation.statementBalance(), reconciliation.calculatedBalance(), reconciliation.difference(),
                  ReconciliationStatus.CLOSED, reconciliation.note(), reconciliation.createdAt(), Instant.now(),
                  reconciliation.version()));
            })))
        .map(this::view);
  }

  private ReconciliationView view(ReconciliationEntity r) {
    return new ReconciliationView(r.id(), r.accountId(), r.statementDate(), r.statementBalance(),
        r.calculatedBalance(), r.difference(), r.status(), r.note(), r.createdAt());
  }
}
