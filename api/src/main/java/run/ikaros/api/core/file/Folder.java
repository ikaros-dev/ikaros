package run.ikaros.api.core.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Folder {
    private Long id;
    private String name;
    @JsonProperty("parent_id")
    private Long parentId;
    @JsonProperty("parent_name")
    private String parentName;
    @JsonProperty("create_time")
    private LocalDateTime createTime;
    @JsonProperty("update_time")
    private LocalDateTime updateTime;
    private List<File> files;
    private List<Folder> folders;

    public boolean hasFile() {
        return !files.isEmpty();
    }

    public boolean hasFolder() {
        return !folders.isEmpty();
    }

}
