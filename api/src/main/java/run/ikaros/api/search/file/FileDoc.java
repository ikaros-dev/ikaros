package run.ikaros.api.search.file;

import lombok.Data;
import run.ikaros.api.store.enums.FileType;

@Data
public class FileDoc {
    private Long id;
    private String name;
    private String fsPath;
    private String url;
    private FileType type;
}
