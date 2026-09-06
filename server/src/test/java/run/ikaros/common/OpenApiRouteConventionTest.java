package run.ikaros.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class OpenApiRouteConventionTest {
    private static final Pattern PATH = Pattern.compile("^  (/[^:]+):\\s*$", Pattern.MULTILINE);

    @Test
    void openApiContractsDoNotExposeV2RoutePrefix() throws Exception {
        Path root = Path.of("docs/00-product-baseline/contracts");
        try (var files = Files.list(root)) {
            files.filter(path -> path.getFileName().toString().startsWith("openapi-")
                    && path.toString().endsWith(".yaml"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        assertFalse(content.contains("/api/v2"),
                            () -> path + " must not expose /api/v2");
                        Matcher matcher = PATH.matcher(content);
                        while (matcher.find()) {
                            assertFalse(matcher.group(1).startsWith("/v2"),
                                () -> path + " contains a versioned API path: " + matcher.group(1));
                        }
                    } catch (Exception exception) {
                        throw new IllegalStateException("Unable to inspect " + path, exception);
                    }
                });
        }
    }
}
