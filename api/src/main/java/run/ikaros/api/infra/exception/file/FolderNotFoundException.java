package run.ikaros.api.infra.exception.file;

import run.ikaros.api.infra.exception.NotFoundException;

public class FolderNotFoundException extends NotFoundException {
    public FolderNotFoundException(String message) {
        super(message);
    }
}
