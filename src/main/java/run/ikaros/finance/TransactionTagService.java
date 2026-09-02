package run.ikaros.finance;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;

@Service
public class TransactionTagService {
  private final FinanceTransactionRepository transactions;
  private final FinanceTagRepository tags;
  private final TransactionTagRepository links;
  private final FinanceLedgerAccessService access;

  public TransactionTagService(FinanceTransactionRepository t, FinanceTagRepository g, TransactionTagRepository l,
      FinanceLedgerAccessService x) {
    transactions = t;
    tags = g;
    links = l;
    access = x;
  }

  public Mono<TransactionTagView> add(UUID actor, UUID transactionId, TagTransactionRequest q) {
    return transaction(transactionId)
        .flatMap(transaction -> access.requireEditor(actor, transaction.ledgerId())
            .then(tags.findById(q.tagId())
                .filter(tag -> tag.ledgerId().equals(transaction.ledgerId()))
                .switchIfEmpty(Mono.error(new NotFoundException("Tag 不存在或不属于该 Ledger"))))
            .flatMap(tag -> links.findByTransactionIdAndTagId(transactionId, q.tagId())
                .flatMap(old -> Mono.<TransactionTagEntity>error(new ConflictException("Transaction 已绑定该 Tag")))
                .switchIfEmpty(links.save(new TransactionTagEntity(null, transactionId, q.tagId(), transaction.ledgerId(),
                    transaction.ownerId(), Instant.now())))))
        .map(this::view);
  }

  public Flux<TransactionTagView> list(UUID actor, UUID transactionId) {
    return transaction(transactionId)
        .flatMapMany(transaction -> access.access(actor, transaction.ledgerId())
            .flatMapMany(role -> links.findAllByTransactionId(transactionId).map(this::view)));
  }

  public Mono<Void> remove(UUID actor, UUID transactionId, UUID tagId) {
    return transaction(transactionId)
        .flatMap(transaction -> access.requireEditor(actor, transaction.ledgerId())
            .then(links.findByTransactionIdAndTagId(transactionId, tagId)
                .switchIfEmpty(Mono.error(new NotFoundException("Transaction Tag 不存在")))
                .flatMap(links::delete)))
        .then();
  }

  private Mono<FinanceTransactionEntity> transaction(UUID id) {
    return transactions.findById(id)
        .switchIfEmpty(Mono.error(new NotFoundException("Transaction 不存在或无权访问")));
  }

  private TransactionTagView view(TransactionTagEntity x) {
    return new TransactionTagView(x.id(), x.transactionId(), x.tagId(), x.createdAt());
  }
}
