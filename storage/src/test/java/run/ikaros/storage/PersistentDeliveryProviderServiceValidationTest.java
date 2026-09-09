package run.ikaros.storage;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.ObjectMapper;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.integration.api.DurableEventPublisher;
import run.ikaros.operations.api.BackgroundTaskService;
import run.ikaros.storage.api.DeliveryProviderType;
import run.ikaros.storage.api.DeliveryProviderWriteRequest;

class PersistentDeliveryProviderServiceValidationTest {
    @Test
    void rejectsNonSecretCredentialReferenceBeforePersistence() {
        DeliveryProviderRepository providers = mock(DeliveryProviderRepository.class);
        PersistentDeliveryProviderService service = service(providers);

        assertThrows(ConflictException.class, () -> service.create(new DeliveryProviderWriteRequest("cdn", DeliveryProviderType.DIRECT,
                "CDN", "plain-secret", java.util.Map.of(), true), "request-1"));
        verifyNoInteractions(providers);
    }

    @Test
    void rejectsMissingProviderWhenEnabling() {
        DeliveryProviderRepository providers = mock(DeliveryProviderRepository.class);
        when(providers.findById(UUID.randomUUID())).thenReturn(Mono.empty());
        PersistentDeliveryProviderService service = service(providers);
        UUID missing = UUID.randomUUID();
        when(providers.findById(missing)).thenReturn(Mono.empty());

        StepVerifier.create(service.enable(missing)).expectError(NotFoundException.class).verify();
    }

    private PersistentDeliveryProviderService service(DeliveryProviderRepository providers) {
        return new PersistentDeliveryProviderService(providers, new ObjectMapper(), mock(DurableEventPublisher.class),
            mock(DeliveryProviderOperationsService.class), mock(MediaDeliveryBindingRepository.class));
    }
}
