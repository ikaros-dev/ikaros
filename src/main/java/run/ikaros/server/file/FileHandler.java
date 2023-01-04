package run.ikaros.server.file;

import java.util.Map;
import org.pf4j.ExtensionPoint;
import org.springframework.http.codec.multipart.FilePart;
import run.ikaros.server.store.entity.FileEntity;

public interface FileHandler extends ExtensionPoint {
    FileEntity upload(UploadContext context);

    FileEntity delete(FileEntity entity);

    interface UploadContext {
        FilePart file();

        Map<String, Object> metadata();
    }

}
