package run.ikaros.server.core.file;

import org.springframework.stereotype.Component;

@Component
public class LocalFileHandler implements FileHandler {
    @Override
    public File upload(UploadContext context) {
        return null;
    }

    @Override
    public File delete(File file) {
        return null;
    }
}
