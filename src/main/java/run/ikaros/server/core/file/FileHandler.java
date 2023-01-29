package run.ikaros.server.core.file;

import java.util.Map;
import org.pf4j.ExtensionPoint;
import org.springframework.http.codec.multipart.FilePart;

public interface FileHandler extends ExtensionPoint {
    /**
     * File upload policy.
     *
     * @return a file upload policy str
     * @see FilePolicy#getName()
     */
    String policy();

    File upload(UploadContext context);

    File delete(File file);

    interface UploadContext {
        FilePart filepart();

        Map<String, Object> metadata();
    }

}
