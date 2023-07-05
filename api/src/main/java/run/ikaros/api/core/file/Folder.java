package run.ikaros.api.core.file;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
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
    @JsonProperty("update_time")
    private LocalDateTime updateTime;
    private List<File> files;
    private List<Folder> folders;
    private boolean canRead;

    public boolean hasFile() {
        return !files.isEmpty();
    }

    public boolean hasFolder() {
        return !folders.isEmpty();
    }

    /**
     * Update can read with files and folders.
     */
    public Folder updateCanRead() {
        boolean result = true;
        if (Objects.nonNull(files)) {
            for (File file : files) {
                if (!file.getCanRead()) {
                    result = false;
                    break;
                }
            }
        }

        if (result && Objects.nonNull(folders)) {
            for (Folder folder : folders) {
                if (!folder.canRead) {
                    result = false;
                    break;
                }
            }
        }

        this.canRead = result;
        return this;
    }

}
