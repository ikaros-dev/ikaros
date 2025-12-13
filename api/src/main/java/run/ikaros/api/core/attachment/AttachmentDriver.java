package run.ikaros.api.core.attachment;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.store.enums.AttachmentDriverType;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AttachmentDriver {
    private Long id;
    /**
     * enable current attachment driver.
     */
    private boolean enable;
    @Schema(requiredMode = REQUIRED, defaultValue = "LOCAL")
    private AttachmentDriverType type;
    private String name;
    @JsonProperty("mount_name")
    private String mountName;
    /**
     * driver remote relative path or sub dir id, null or empty is root dir.
     */
    @JsonProperty("remote_path")
    private String remotePath;
    private Long order;
    private String comment;
    @JsonProperty("refresh_token")
    private String refreshToken;
    @JsonProperty("access_token")
    private String accessToken;
    @JsonProperty("expire_time")
    private LocalDateTime expireTime;
    @JsonProperty("list_page_size")
    private Long listPageSize;
    @JsonProperty("root_dir_id")
    private String rootDirId;
    /**
     * api request limit r/1s, default 0.1 r/1s.
     */
    @JsonProperty("request_limit")
    private Double requestLimit;

    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("user_name")
    private String username;
    private String avatar;
    @JsonProperty("space_total")
    private Long spaceTotal;
    @JsonProperty("space_use")
    private Long spaceUse;
}
