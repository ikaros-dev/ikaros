package run.ikaros.server.core.file;

import java.util.Map;
import org.pf4j.ExtensionPoint;
import org.springframework.http.codec.multipart.FilePart;
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
        FilePart filepart();

        Map<String, Object> metadata();

        String policy();
    }

    record DefaultUploadContext(FilePart filePart, String policy, Map<String, Object> metadata)
        implements UploadContext {

        @Override
        public FilePart filepart() {
            return filePart;
        }

        @Override
        public Map<String, Object> metadata() {
            return metadata;
        }

        @Override
        public String policy() {
            return policy;
        }
    }

}
