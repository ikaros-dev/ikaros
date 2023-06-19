package run.ikaros.api.core.file;

import java.util.Map;
import org.pf4j.ExtensionPoint;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FileHandler extends ExtensionPoint {
    /**
     * File upload policy.
     *
     * @return a file upload policy str
     * @see FilePolicy#getName()
     */
    String policy();

    Mono<File> upload(UploadContext context);

    default Mono<File> uploadBySharding() {
        // TODO large multipart file upload support
        return null;
    }


    Mono<File> delete(File file);

    interface UploadContext {
        Flux<DataBuffer> dataBuffer();

        Map<String, Object> metadata();

        String policy();

        String fileName();

    }

    record DateBufferUploadContext(String fileName,
                                   Flux<DataBuffer> dataBufferFlux,
                                   String policy,
                                   Map<String, Object> metadata)
        implements UploadContext {

        @Override
        public Flux<DataBuffer> dataBuffer() {
            return dataBufferFlux;
        }

        @Override
        public Map<String, Object> metadata() {
            return metadata;
        }

        @Override
        public String policy() {
            return policy;
        }

        @Override
        public String fileName() {
            return fileName;
        }
    }


    record DefaultUploadContext(FilePart filePart, String policy, Map<String, Object> metadata)
        implements UploadContext {

        @Override
        public Flux<DataBuffer> dataBuffer() {
            DataBufferFactory bufferFactory = new DefaultDataBufferFactory();
            return filePart.content().map(dataBuffer ->
                bufferFactory.wrap(dataBuffer.toByteBuffer()));
        }

        @Override
        public Map<String, Object> metadata() {
            return metadata;
        }

        @Override
        public String policy() {
            return policy;
        }

        @Override
        public String fileName() {
            return filePart.filename();
        }
    }

}
