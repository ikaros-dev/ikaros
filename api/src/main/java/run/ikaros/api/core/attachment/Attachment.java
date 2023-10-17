package run.ikaros.api.core.attachment;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import run.ikaros.api.store.enums.AttachmentType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Attachment {
    @Id
    private Long id;
    @JsonProperty("parent_id")
    private Long parentId;
    private AttachmentType type;
    /**
     * HTTP path.
     */
    private String url;
    /**
     * File path in file system.
     */
    @JsonProperty("fs_path")
    private String fsPath;
    /**
     * filename with postfix.
     */
    private String name;
    private Long size;
    @JsonProperty("update_time")
    private LocalDateTime updateTime;
}
