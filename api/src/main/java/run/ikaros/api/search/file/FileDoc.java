package run.ikaros.api.search.file;

import lombok.Data;
import run.ikaros.api.store.enums.FileType;

@Data
public class FileDoc {
    private Long id;
    private String name;
    private String originalPath;
    private String url;
    private FileType type;
    private String originalName;
}
