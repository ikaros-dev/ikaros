package run.ikaros.common;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ControllerArchitectureTest {
    @Test
    void controllersDoNotDependOnRepositories() throws IOException {
        Path repositoryRoot = Path.of("").toAbsolutePath();
        while (repositoryRoot != null && !Files.isDirectory(repositoryRoot.resolve("application/src/main/java"))) {
            repositoryRoot = repositoryRoot.getParent();
        }
        if (repositoryRoot == null) throw new IllegalStateException("Unable to locate repository root");
        List<Path> sourceRoots;
        try (Stream<Path> modules = Files.list(repositoryRoot)) {
            sourceRoots = modules.map(path -> path.resolve("src/main/java"))
                .filter(Files::isDirectory).toList();
        }
        List<Path> controllers = new ArrayList<>();
        for (Path sourceRoot : sourceRoots) {
            try (Stream<Path> files = Files.walk(sourceRoot)) {
                files.filter(path -> path.toString().endsWith("Controller.java")).forEach(controllers::add);
            }
        }

        assertFalse(controllers.isEmpty(), "No production Controller sources were scanned");
        for (Path controller : controllers) {
            String source = Files.readString(controller);
            assertFalse(source.contains("Repository"),
                () -> controller + " must depend on an application service, not a repository");
        }
    }
}
