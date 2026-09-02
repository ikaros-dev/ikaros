package run.ikaros.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class Sha256BlobIntegrityServiceTest {
    private final Sha256BlobIntegrityService service = new Sha256BlobIntegrityService();

    @Test
    void verifiesMatchingContent() {
        byte[] bytes = "ikaros".getBytes(StandardCharsets.UTF_8);
        assertEquals(BlobIntegrityStatus.VERIFIED, service.verify(UUID.randomUUID(),
            "5a4ee8bd994c1c47f259c8bdaf55e881ea91bd31b9bc5e02f8f21c30fc4e1da3", bytes.length,
            new ByteArrayInputStream(bytes)).map(BlobIntegrityResult::status).block());
    }

    @Test
    void marksWrongDigestCorrupt() {
        byte[] bytes = "ikaros".getBytes(StandardCharsets.UTF_8);
        assertEquals(BlobIntegrityStatus.CORRUPT, service.verify(UUID.randomUUID(), "00", bytes.length,
            new ByteArrayInputStream(bytes)).map(BlobIntegrityResult::status).block());
    }
}
