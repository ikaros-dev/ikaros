package run.ikaros.server.infra.utils;

import java.io.File;
import java.nio.file.Path;
import reactor.test.StepVerifier;

class DataBufferFileWriterTest {

    // @Test
    void zipDirectoryToStream() {
        Path path =
            new File("C:\\Users\\chivehao\\Downloads\\019b6eb0-ffb2-7d80-a190-cf38b5ae7ac4")
                .toPath();
        Path targetPaht =
            new File("C:\\Users\\chivehao\\Downloads\\019b6eb0-ffb2-7d80-a190-cf38b5ae7ac4.zip")
                .toPath();
        StepVerifier.create(DataBufferFileWriter.writeFluxToFile(
            DataBufferFileWriter.zipDirectoryToStream(path.toFile().getAbsolutePath()),
            targetPaht.toFile().getAbsolutePath()
        )).verifyComplete();
    }
}