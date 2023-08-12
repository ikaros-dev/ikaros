package run.ikaros.server.core.file.event;

import java.time.Clock;
import run.ikaros.server.store.entity.FileEntity;

/**
 * File save event, contain create and update.
 */
public class FileSaveEvent extends FileChangeEvent {

    public FileSaveEvent(Object source, FileEntity fileEntity) {
        super(source, fileEntity);
    }

    public FileSaveEvent(Object source, Clock clock, FileEntity fileEntity) {
        super(source, clock, fileEntity);
    }
}
