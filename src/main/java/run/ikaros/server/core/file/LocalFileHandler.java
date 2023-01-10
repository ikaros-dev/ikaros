package run.ikaros.server.core.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import run.ikaros.server.infra.properties.IkarosProperties;

@Slf4j
@Component
public class LocalFileHandler implements FileHandler {
    private final IkarosProperties ikarosProp;

    public LocalFileHandler(IkarosProperties ikarosProp) {
        this.ikarosProp = ikarosProp;
    }

    @Override
    public File upload(UploadContext context) {
        return null;
    }

    @Override
    public File delete(File file) {
        return null;
    }
}
