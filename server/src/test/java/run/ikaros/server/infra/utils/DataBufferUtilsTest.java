package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

class DataBufferUtilsTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldUploadDataBuffers() throws IOException {
        Path targetFile = tempDir.resolve("test.txt");
        DefaultDataBufferFactory factory = DefaultDataBufferFactory.sharedInstance;
        DataBuffer buffer1 = factory.wrap("Hello".getBytes(StandardCharsets.UTF_8));
        DataBuffer buffer2 = factory.wrap("World".getBytes(StandardCharsets.UTF_8));
        Flux<DataBuffer> dataBufferFlux = Flux.just(buffer1, buffer2);

        StepVerifier.create(DataBufferUtils.uploadDataBuffers(dataBufferFlux, targetFile))
            .expectNextCount(1)
            .verifyComplete();

        // 由于并发写入，顺序不确定，只验证内容包含所有数据
        String content = Files.readString(targetFile);
        assertThat(content).contains("Hello", "World");
        assertThat(content).hasSize(10);
    }

    @Test
    void shouldHandleEmptyDataBufferFlux() throws IOException {
        Path targetFile = tempDir.resolve("empty.txt");
        Flux<DataBuffer> dataBufferFlux = Flux.empty();

        StepVerifier.create(DataBufferUtils.uploadDataBuffers(dataBufferFlux, targetFile))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void shouldUploadSingleDataBuffer() throws IOException {
        Path targetFile = tempDir.resolve("single.txt");
        DefaultDataBufferFactory factory = DefaultDataBufferFactory.sharedInstance;
        DataBuffer buffer = factory.wrap("Single Buffer".getBytes(StandardCharsets.UTF_8));
        Flux<DataBuffer> dataBufferFlux = Flux.just(buffer);

        StepVerifier.create(DataBufferUtils.uploadDataBuffers(dataBufferFlux, targetFile))
            .expectNextCount(1)
            .verifyComplete();

        assertThat(Files.readString(targetFile)).isEqualTo("Single Buffer");
    }
}
