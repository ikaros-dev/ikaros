package run.ikaros.common;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class HttpOperationRegistryTest {
    private static final Pattern SOURCE = Pattern.compile("source:\\s*([^}\\s]+)");

    @Test
    void everyRegisteredOpenApiSourceExists() throws Exception {
        Path registry = Path.of("docs/00-product-baseline/contracts/P0-HTTP-Operation-Registry.yaml");
        String content = Files.readString(registry);
        Matcher matcher = SOURCE.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
            Path source = Path.of("docs/00-product-baseline").resolve(matcher.group(1));
            assertTrue(Files.isRegularFile(source), () -> "missing operation source: " + source);
        }
        assertTrue(count > 0, "operation registry must declare at least one source");
    }

    @Test
    void registeredPathsDoNotExposeVersionedApiPrefix() throws Exception {
        Path registry = Path.of("docs/00-product-baseline/contracts/P0-HTTP-Operation-Registry.yaml");
        String content = Files.readString(registry);
        assertFalse(content.contains("/api/v2"), "operation registry must not expose /api/v2");
        assertFalse(content.matches("(?s).*path:\\s*/v2(?:[/\\s}].*)?"),
            "operation registry must not expose a /v2 path");
    }
}
