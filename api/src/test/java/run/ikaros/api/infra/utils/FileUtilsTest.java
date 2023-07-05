package run.ikaros.api.infra.utils;


import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class FileUtilsTest {
    String fileName = "2023-06-19 10-16-48.mp4";

    @Test
    @Disabled
    void splitFile() throws URISyntaxException {
        Path filePath = Path.of(
            new File("C:\\Users\\li-guohao\\Videos\\tests\\original\\" + fileName).toURI());
        Path targetDirPath = Path.of(
            new File("C:\\Users\\li-guohao\\Videos\\tests\\split").toURI()
        );
        List<Path> paths = FileUtils.split(filePath, targetDirPath, 1024 * 10);
        Assertions.assertThat(paths.size()).isEqualTo(6);
        paths.forEach(System.out::println);
    }

    @Test
    @Disabled
    void synthesize() {
        Path chunkFilesDirPath = Path.of(
            new File("C:\\Users\\li-guohao\\Videos\\tests\\split").toURI()
        );
        Path targetFilePath = Path.of(
            new File("C:\\Users\\li-guohao\\Videos\\tests\\synthesize\\" + fileName).toURI());
        List<Path> chunkPaths =
            Arrays.stream(Objects.requireNonNull(chunkFilesDirPath.toFile().listFiles()))
                .map(file -> Path.of(file.toURI()))
                .toList();
        FileUtils.synthesize(chunkPaths, targetFilePath);
    }

    @Test
    @Disabled
    void url2path() {
        Path basicPath = Path.of("C:\\Users\\li-guohao\\Videos");
        String url = "/tests/2023-06-15 09-56-02.mp4";
        String result = FileUtils.url2path(url, basicPath);
        System.out.println(result);
    }
}