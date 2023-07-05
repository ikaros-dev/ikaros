package run.ikaros.api.core.file;


import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.FileType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class File {
    private Long id;
    private Long folderId;
    private String url;
    private String name;
    private String md5;
    private String aesKey;
    private Long size;
    private FileType type;
    private String fsPath;
    private Boolean canRead;
    private LocalDateTime updateTime;
}
