package run.ikaros.server.infra.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class DataBufferUtils {

    public static Flux<DataBuffer> formFilePart(FilePart filePart) {
        return filePart.content();
    }

    /**
     * Upload data buffer flux to target path.
     */
    public static Mono<Long> uploadDataBuffers(Flux<DataBuffer> dataBufferFlux, Path targetPath) {
        AtomicReference<Long> size = new AtomicReference<>(0L);
        return dataBufferFlux
            .flatMapSequential(dataBuffer -> Mono.fromRunnable(() -> {
                try {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    Files.write(targetPath, bytes, StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
                    Long oldSize = size.get();
                    size.set(oldSize + bytes.length);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to upload data buffer", e);
                }
            }).subscribeOn(Schedulers.boundedElastic()))
            .then(Mono.just(size.get()));
    }
}
