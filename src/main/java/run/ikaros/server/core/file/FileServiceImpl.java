package run.ikaros.server.core.file;

import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl implements FileService {
    private final FileHandler fileHandler;

    public FileServiceImpl(FileHandler fileHandler) {
        this.fileHandler = fileHandler;
    }

}
