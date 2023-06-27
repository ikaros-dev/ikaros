package run.ikaros.server.search.file;

import run.ikaros.api.search.file.FileDoc;
import run.ikaros.api.store.entity.FileEntity;

public class FileDocConverter {
    /**
     * Get {@link FileDoc} from {@link  FileEntity}.
     */
    public static FileDoc fromEntity(FileEntity entity) {
        FileDoc fileDoc = new FileDoc();
        fileDoc.setId(entity.getId());
        fileDoc.setName(entity.getName());
        fileDoc.setOriginalPath(entity.getOriginalPath());
        fileDoc.setUrl(entity.getUrl());
        fileDoc.setType(entity.getType());
        fileDoc.setOriginalName(entity.getOriginalName());
        return fileDoc;
    }

}
