package run.ikaros.common;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ControllerRouteConventionTest {
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
}
