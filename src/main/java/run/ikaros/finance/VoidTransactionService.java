package run.ikaros.finance;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class VoidTransactionService {
  private final FinanceTransactionRepository transactions;
  private final AccountRepository accounts;
  private final FinanceLedgerAccessService access;

  public VoidTransactionService(FinanceTransactionRepository t, AccountRepository a, FinanceLedgerAccessService x) {
    transactions = t;
    accounts = a;
    access = x;
  }

  public Mono<TransactionView> voidTransaction(UUID actor, UUID id) {
    return transactions.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Transaction 不存在或无权访问")))
        .flatMap(t -> access.requireEditor(actor, t.ledgerId()).then(Mono.defer(() -> {
          if (t.status() != TransactionStatus.POSTED) {
            return Mono.error(new ConflictException("Transaction 已经不是 POSTED 状态"));
          }
          return accounts.findById(t.accountId())
              .filter(a -> a.ledgerId().equals(t.ledgerId()))
              .switchIfEmpty(Mono.error(new NotFoundException("源 Account 不存在")))
              .flatMap(source -> {
                Mono<Void> reverse = t.type() == TransactionType.TRANSFER
                    ? accounts.findById(t.targetAccountId())
                        .filter(a -> a.ledgerId().equals(t.ledgerId()))
                        .switchIfEmpty(Mono.error(new NotFoundException("目标 Account 不存在")))
                        .flatMap(target -> reverse(source, target, t.amount()))
                    : reverse(source, null, t.amount(), t.type());
                return reverse.then(transactions.save(new FinanceTransactionEntity(
                    t.id(), t.ledgerId(), t.ownerId(), t.type(), t.accountId(), t.targetAccountId(),
                    t.amount(), t.currency(), t.categoryId(), t.payee(), t.note(), t.occurredAt(),
                    TransactionStatus.VOIDED, t.source(), t.createdAt(), t.version())));
              });
        })))
        .map(this::view);
  }

  private Mono<Void> reverse(AccountEntity source, AccountEntity target, BigDecimal amount) {
    return save(source, source.currentBalance().add(amount))
        .then(save(target, target.currentBalance().subtract(amount)));
  }

  private Mono<Void> reverse(AccountEntity source, AccountEntity target, BigDecimal amount, TransactionType type) {
    BigDecimal delta = type == TransactionType.INCOME ? amount : type == TransactionType.EXPENSE ? amount.negate() : amount;
    return save(source, source.currentBalance().subtract(delta));
  }

  private Mono<Void> save(AccountEntity account, BigDecimal balance) {
    return accounts.save(new AccountEntity(account.id(), account.ledgerId(), account.ownerId(), account.name(),
        account.type(), account.currency(), account.openingBalance(), balance, account.institution(),
        account.maskedIdentifier(), account.credentialRef(), account.archived(), account.createdAt(),
        account.updatedAt(), account.version())).then();
  }

  private TransactionView view(FinanceTransactionEntity x) {
    return new TransactionView(x.id(), x.ledgerId(), x.type(), x.accountId(), x.targetAccountId(), x.amount(),
        x.currency(), x.categoryId(), x.payee(), x.note(), x.occurredAt(), x.status(), x.source());
  }
}
