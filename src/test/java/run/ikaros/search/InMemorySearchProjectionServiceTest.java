package run.ikaros.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemorySearchProjectionServiceTest {
    @Test
    void ignoresOutOfOrderSourceVersionsAndAdvancesRebuildGeneration() {
        InMemorySearchProjectionService service = new InMemorySearchProjectionService();
        UUID source = UUID.randomUUID();
        service.project(source, 2, Map.of("title", "new"), "1", 1).block();
        service.project(source, 1, Map.of("title", "old"), "1", 1).block();
        assertEquals(2, service.get(source).block().sourceVersion());
        assertEquals(1, service.startRebuild().block());
    }
}
