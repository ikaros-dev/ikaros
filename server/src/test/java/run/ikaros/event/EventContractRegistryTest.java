package run.ikaros.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

class EventContractRegistryTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void eventTypeAndSchemaVersionRegistryIsUnique() throws Exception {
        Path schema = Path.of("docs/00-product-baseline/contracts/schema/p0-event-v1.schema.json");
        JsonNode root = mapper.readTree(Files.readString(schema));
        JsonNode eventTypes = root.path("properties").path("event_type").path("enum");

        assertTrue(eventTypes.isArray(), "event_type must declare an enum registry");
        Set<String> uniqueTypes = new HashSet<>();
        eventTypes.forEach(value -> assertTrue(uniqueTypes.add(value.asString()),
            () -> "duplicate event_type registration: " + value.asString()));
        assertEquals(eventTypes.size(), uniqueTypes.size());
        assertEquals(1, root.path("properties").path("schema_version").path("const").asInt());
    }
}
