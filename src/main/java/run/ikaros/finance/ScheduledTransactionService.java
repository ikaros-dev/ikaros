package run.ikaros.finance;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class ScheduledTransactionService {
  private final ScheduledTransactionRepository schedules;
  private final AccountRepository accounts;
  private final FinanceLedgerAccessService access;

  public ScheduledTransactionService(ScheduledTransactionRepository s, AccountRepository a, FinanceLedgerAccessService x) {
    schedules = s;
    accounts = a;
    access = x;
  }

  public Mono<ScheduledTransactionView> create(UUID actor, UUID ledgerId, CreateScheduledTransactionRequest q) {
    if (q.amount().signum() <= 0) {
      return Mono.error(new IllegalArgumentException("计划交易金额必须大于 0"));
    }
    return access.requireEditor(actor, ledgerId)
        .then(account(ledgerId, q.accountId()))
        .flatMap(source -> {
          Mono<Void> targetCheck = q.type() == TransactionType.TRANSFER
              ? account(ledgerId, q.targetAccountId()).then()
              : Mono.empty();
          return targetCheck.then(schedules.save(new ScheduledTransactionEntity(null, ledgerId, actor, q.rule(),
              q.nextRun(), q.type(), q.accountId(), q.targetAccountId(), q.amount(),
              q.currency() == null ? source.currency() : q.currency().toUpperCase(), q.categoryId(), q.payee(),
              q.note(), true, Instant.now(), Instant.now(), null)));
        })
        .map(this::view);
  }

  public Flux<ScheduledTransactionView> list(UUID actor, UUID ledgerId) {
    return access.access(actor, ledgerId)
        .flatMapMany(role -> schedules.findAllByLedgerIdOrderByNextRunAsc(ledgerId).map(this::view));
  }

  public Mono<ScheduledTransactionView> setActive(UUID actor, UUID id, boolean active) {
    return schedules.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Scheduled Transaction 不存在或无权访问")))
        .flatMap(schedule -> access.requireEditor(actor, schedule.ledgerId())
            .then(schedules.save(new ScheduledTransactionEntity(schedule.id(), schedule.ledgerId(), schedule.ownerId(),
                schedule.rule(), schedule.nextRun(), schedule.type(), schedule.accountId(), schedule.targetAccountId(),
                schedule.amount(), schedule.currency(), schedule.categoryId(), schedule.payee(), schedule.note(), active,
                schedule.createdAt(), Instant.now(), schedule.version()))))
        .map(this::view);
  }

  private Mono<AccountEntity> account(UUID ledgerId, UUID id) {
    return accounts.findById(id)
        .filter(account -> account.ledgerId().equals(ledgerId))
        .switchIfEmpty(Mono.error(new NotFoundException("Account 不存在或不属于该 Ledger")));
  }

  private ScheduledTransactionView view(ScheduledTransactionEntity s) {
    return new ScheduledTransactionView(s.id(), s.ledgerId(), s.rule(), s.nextRun(), s.type(), s.accountId(),
        s.targetAccountId(), s.amount(), s.currency(), s.categoryId(), s.payee(), s.note(), s.active());
  }
}
