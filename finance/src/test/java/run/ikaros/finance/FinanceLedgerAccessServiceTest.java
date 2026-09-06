package run.ikaros.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/** 验证 Ledger Owner、Editor、Viewer 的访问边界。 */
class FinanceLedgerAccessServiceTest {
  private LedgerRepository ledgers;
  private LedgerMemberRepository members;
  private FinanceLedgerAccessService service;

  @BeforeEach
  void setUp() {
    ledgers = mock(LedgerRepository.class);
    members = mock(LedgerMemberRepository.class);
    service = new FinanceLedgerAccessService(ledgers, members);
  }

  @Test
  void ownerHasEditorAccessWithoutMembershipLookup() {
    UUID ledgerId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    when(ledgers.findById(ledgerId)).thenReturn(Mono.just(ledger(ledgerId, ownerId)));

    StepVerifier.create(service.access(ownerId, ledgerId))
        .assertNext(role -> assertThat(role).isEqualTo(LedgerMemberRole.EDITOR))
        .verifyComplete();
  }

  @Test
  void memberReceivesStoredRole() {
    UUID ledgerId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID memberId = UUID.randomUUID();
    when(ledgers.findById(ledgerId)).thenReturn(Mono.just(ledger(ledgerId, ownerId)));
    when(members.findByLedgerIdAndPrincipalId(ledgerId, memberId)).thenReturn(
        Mono.just(new LedgerMemberEntity(UUID.randomUUID(), ledgerId, memberId, ownerId,
            LedgerMemberRole.VIEWER, Instant.now(), 0L)));

    StepVerifier.create(service.access(memberId, ledgerId))
        .assertNext(role -> assertThat(role).isEqualTo(LedgerMemberRole.VIEWER))
        .verifyComplete();
  }

  @Test
  void missingMemberCannotWrite() {
    UUID ledgerId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID outsiderId = UUID.randomUUID();
    when(ledgers.findById(ledgerId)).thenReturn(Mono.just(ledger(ledgerId, ownerId)));
    when(members.findByLedgerIdAndPrincipalId(ledgerId, outsiderId)).thenReturn(Mono.empty());

    StepVerifier.create(service.requireEditor(outsiderId, ledgerId))
        .expectError()
        .verify();
  }

  private LedgerEntity ledger(UUID ledgerId, UUID ownerId) {
    Instant now = Instant.now();
    return new LedgerEntity(ledgerId, ownerId, "Ledger", "CNY", false, now, now, 0L);
  }
}
