package run.ikaros.ingestion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import run.ikaros.audit.AuditService;

class DefaultIngestionSourceServiceTest {
    @Mock private IngestionSourceRepository repository;
    @Mock private AuditService auditService;
    private DefaultIngestionSourceService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefaultIngestionSourceService(repository, auditService, new ObjectMapper());
        when(auditService.record(any(), any(), any(), any(), any())).thenReturn(Mono.empty());
    }

    @Test
    void rejectsPlaintextCredentialReference() {
        CreateIngestionSourceRequest request = new CreateIngestionSourceRequest(
            IngestionSourceType.LOCAL_FILESYSTEM, "Media", "C:/media", "plaintext", Map.of());
        assertThrows(RuntimeException.class, () -> service.create(UUID.randomUUID(), request).block());
    }

    @Test
    void createsAndDisablesOwnedSourceWithoutExposingCredential() {
        UUID ownerId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        Instant now = Instant.now();
        IngestionSourceEntity source = new IngestionSourceEntity(sourceId, ownerId, "LOCAL_FILESYSTEM", "Media",
            "C:/media", "secret://media", "{\"include\":\"*.mkv\"}", "ENABLED", null, "UNKNOWN",
            now, now, 0L);
        when(repository.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        assertEquals(true, service.create(ownerId, new CreateIngestionSourceRequest(
            IngestionSourceType.LOCAL_FILESYSTEM, "Media", "C:/media", "secret://media",
            Map.of("include", "*.mkv"))).block().credentialConfigured());
        when(repository.findByIdAndOwnerId(sourceId, ownerId)).thenReturn(Mono.just(source));
        assertEquals(IngestionSourceStatus.DISABLED, service.disable(ownerId, sourceId).block().status());
    }
}
