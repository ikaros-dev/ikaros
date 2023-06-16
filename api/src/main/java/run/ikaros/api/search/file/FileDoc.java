package run.ikaros.api.search.file;

import lombok.Data;
import run.ikaros.api.store.enums.FilePlace;
import run.ikaros.api.store.enums.FileType;

@Data
public class FileDoc {
    private String name;
    private String url;
    private FileType type;
    private FilePlace place;
}
