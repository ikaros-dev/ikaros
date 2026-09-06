package run.ikaros.event;

import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.reactive.TransactionalOperator;
import static org.mockito.Mockito.mock;
import run.ikaros.integration.api.EventAppendRequest;

class DurableEventServiceTest {
    @Test
    void rejectsSecretLikePayloadsBeforePersistence() {
        DurableEventService service = new DurableEventService(mock(OutboxEventRepository.class),
            mock(InboxEntryRepository.class), mock(TransactionalOperator.class));
        assertThrows(RuntimeException.class, () -> service.append(new EventAppendRequest("resource.resource.created", 1,
            "resource", "resource", UUID.randomUUID(), "{\"access_token\":\"x\"}")).block());
    }

    @Test
    void rejectsMalformedOrNonObjectPayloadsBeforePersistence() {
        DurableEventService service = new DurableEventService(mock(OutboxEventRepository.class),
            mock(InboxEntryRepository.class), mock(TransactionalOperator.class));
        assertThrows(RuntimeException.class, () -> service.append(new EventAppendRequest("resource.resource.created", 1,
            "resource", "resource", UUID.randomUUID(), "not-json")).block());
        assertThrows(RuntimeException.class, () -> service.append(new EventAppendRequest("resource.resource.created", 1,
            "resource", "resource", UUID.randomUUID(), "[]")).block());
    }

    @Test
    void rejectsUnstableEventTypeNamesBeforePersistence() {
        DurableEventService service = new DurableEventService(mock(OutboxEventRepository.class),
            mock(InboxEntryRepository.class), mock(TransactionalOperator.class));
        assertThrows(RuntimeException.class, () -> service.append(new EventAppendRequest("ResourceCreated", 1,
            "resource", "resource", UUID.randomUUID(), "{}")).block());
    }
}
