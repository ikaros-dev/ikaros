package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.ConflictException;
import run.ikaros.event.DurableEventService;

class StorageRestoreBudgetServiceTest {
    private final StorageRestoreBudgetRepository budgets = mock(StorageRestoreBudgetRepository.class);
    private final StorageRestoreBudgetService service = new StorageRestoreBudgetService(budgets,
        mock(DurableEventService.class));

    @Test
    void confirmationIsRequiredWhenRequestExceedsSingleRequestBudget() {
        when(budgets.findById(StorageRestoreBudgetService.DEFAULT_ID)).thenReturn(Mono.just(budget(
            StorageRestoreBudgetAction.REQUIRE_CONFIRMATION, 10)));

        StepVerifier.create(service.check(1, 11))
            .expectErrorSatisfies(error -> assertInstanceOf(ConflictException.class, error))
            .verify();
    }

    @Test
    void confirmationAllowsExplicitOverrideOfSingleRequestBudget() {
        when(budgets.findById(StorageRestoreBudgetService.DEFAULT_ID)).thenReturn(Mono.just(budget(
            StorageRestoreBudgetAction.REQUIRE_CONFIRMATION, 10)));

        StepVerifier.create(service.check(1, 11, "confirmation-ref"))
            .verifyComplete();
    }

    @Test
    void rejectPolicyDoesNotAcceptConfirmationToken() {
        when(budgets.findById(StorageRestoreBudgetService.DEFAULT_ID)).thenReturn(Mono.just(budget(
            StorageRestoreBudgetAction.REJECT, 10)));

        StepVerifier.create(service.check(1, 11, "confirmation-ref"))
            .expectErrorSatisfies(error -> assertInstanceOf(ConflictException.class, error))
            .verify();
    }

    private StorageRestoreBudgetEntity budget(StorageRestoreBudgetAction action, long maxBytes) {
        return new StorageRestoreBudgetEntity(StorageRestoreBudgetService.DEFAULT_ID, maxBytes, 10,
            2, 100, 1000, 1000, action, Instant.now(), 0L);
    }
}
