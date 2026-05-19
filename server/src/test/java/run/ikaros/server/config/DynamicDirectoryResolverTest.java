package run.ikaros.server.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.reactive.resource.ResourceResolverChain;
import org.springframework.web.server.ServerWebExchange;

class DynamicDirectoryResolverTest {

    private DynamicDirectoryResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new DynamicDirectoryResolver();
    }

    @Test
    void constructor_noArg_createsInstance() {
        assertThat(resolver).isNotNull();
        assertThat(resolver).isInstanceOf(DynamicDirectoryResolver.class);
    }

    @Test
    void addDirectoryMapping_addsMapping(@TempDir Path tempDir) {
        resolver.addDirectoryMapping("/uploads/", tempDir.toString());

        Map<String, Path> mappings = resolver.getAllMappings();
        assertThat(mappings).containsKey("/uploads/");
        assertThat(mappings.get("/uploads/")).isEqualTo(
            tempDir.toAbsolutePath().normalize());
    }

    @Test
    void removeDirectoryMapping_removesMapping(@TempDir Path tempDir) {
        resolver.addDirectoryMapping("/uploads/", tempDir.toString());
        assertThat(resolver.getAllMappings()).containsKey("/uploads/");

        resolver.removeDirectoryMapping("/uploads/");

        assertThat(resolver.getAllMappings()).doesNotContainKey("/uploads/");
    }

    @Test
    void getAllMappings_returnsAllMappings(@TempDir Path tempDir) throws IOException {
        Path dir1 = Files.createTempDirectory(tempDir, "dir1");
        Path dir2 = Files.createTempDirectory(tempDir, "dir2");

        resolver.addDirectoryMapping("/path1/", dir1.toString());
        resolver.addDirectoryMapping("/path2/", dir2.toString());

        Map<String, Path> mappings = resolver.getAllMappings();
        assertThat(mappings).hasSize(2);
        assertThat(mappings).containsKey("/path1/");
        assertThat(mappings).containsKey("/path2/");
    }

    @Test
    void getAllMappings_returnsEmptyMapWhenNoMappings() {
        Map<String, Path> mappings = resolver.getAllMappings();
        assertThat(mappings).isEmpty();
    }

    @Test
    void resolveResource_returnsFileSystemResourceForMappedPath(
        @TempDir Path tempDir) throws IOException {
        // Create a real file in the temp directory
        Path file = Files.createFile(tempDir.resolve("test.txt"));
        Files.writeString(file, "hello");

        resolver.addDirectoryMapping("/uploads/", tempDir.toString());

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ResourceResolverChain chain = mock(ResourceResolverChain.class);

        Resource resource = resolver.resolveResource(
            exchange, "/uploads/test.txt", List.of(), chain).block();

        assertThat(resource).isNotNull();
        assertThat(resource).isInstanceOf(FileSystemResource.class);
        assertThat(resource.getFilename()).isEqualTo("test.txt");
    }

    @Test
    void resolveResource_delegatesToFallbackForUnmappedPath() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ResourceResolverChain chain = mock(ResourceResolverChain.class);

        // Request a path that has no mapping - should delegate to fallback resolver
        Resource resource = resolver.resolveResource(
            exchange, "/nonexistent/file.txt", List.of(), chain).block();

        // Fallback resolver returns null when file doesn't exist
        assertThat(resource).isNull();
    }

    @Test
    void resolveResource_handlesPathTraversalAttempt(
        @TempDir Path tempDir) throws IOException {
        Path file = Files.createFile(tempDir.resolve("secret.txt"));
        Files.writeString(file, "secret");

        resolver.addDirectoryMapping("/uploads/", tempDir.toString());

        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ResourceResolverChain chain = mock(ResourceResolverChain.class);

        // Attempt path traversal: /uploads/../../secret.txt
        Resource resource = resolver.resolveResource(
            exchange, "/uploads/../../secret.txt", List.of(), chain).block();

        // Should return null because the resolved path is outside the base directory
        assertThat(resource).isNull();
    }

    @Test
    void resolveUrlPath_delegatesToFallback() {
        ResourceResolverChain chain = mock(ResourceResolverChain.class);

        String urlPath = resolver.resolveUrlPath(
            "/some/path", List.of(), chain).block();

        assertThat(urlPath).isNull();
    }
}
