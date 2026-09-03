package run.ikaros.common;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ControllerRouteConventionTest {
    private static final Pattern ROUTE_LITERAL = Pattern.compile(
        "@(RequestMapping|GetMapping|PostMapping|PutMapping|PatchMapping|DeleteMapping)\\s*\\([^)]*?\\\"(/[^\\\"]*)\\\"",
        Pattern.DOTALL);

    @Test
    void controllersMustNotExposeV2Prefix() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith("Controller.java"))
                .forEach(path -> {
                    try {
                        String source = Files.readString(path);
                        assertFalse(source.contains("/v2"),
                            () -> path + " must not contain a /v2 controller route");
                    } catch (IOException exception) {
                        throw new IllegalStateException("Unable to inspect " + path, exception);
                    }
                });
        }
    }

    @Test
    void everyControllerRouteMustUseApiPrefix() throws IOException {
        Path sourceRoot = Path.of("src/main/java");
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            files.filter(path -> path.toString().endsWith("Controller.java"))
                .forEach(path -> {
                    try {
                        String source = Files.readString(path);
                        Matcher matcher = ROUTE_LITERAL.matcher(source);
                        boolean found = false;
                        while (matcher.find()) {
                            found = true;
                            String route = matcher.group(2);
                            if (matcher.group(1).equals("RequestMapping") || route.equals("/api") || route.startsWith("/api/")) {
                                assertTrue(route.equals("/api") || route.startsWith("/api/"),
                                    () -> path + " exposes a class or absolute route outside /api: " + route);
                            }
                        }
                        assertTrue(found, () -> path + " must declare at least one HTTP route");
                    } catch (IOException exception) {
                        throw new IllegalStateException("Unable to inspect " + path, exception);
                    }
                });
        }
    }
}
