package run.ikaros.api.core.file;

import lombok.Data;

@Data
public class RemoteFileChunk {
    private String fileId;
    private String md5;
    private String fileName;
    private Integer category;
    private String path;
    private Long size;
    private Boolean isDir;
}
