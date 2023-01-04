package run.ikaros.server.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.ikaros.server.store.entity.FileEntity;

@Slf4j
@Component
public class LocalFileHandler implements FileHandler {
    @Override
    public FileEntity upload(UploadContext context) {
        return null;
    }

    @Override
    public FileEntity delete(FileEntity entity) {
        return null;
    }
}
