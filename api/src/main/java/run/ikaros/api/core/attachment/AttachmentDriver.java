package run.ikaros.api.core.attachment;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
import run.ikaros.api.store.enums.AttachmentDriverType;

@Data
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AttachmentDriver {
    private @Nullable UUID id;
    /**
     * enable current attachment driver.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY)
    private boolean enable;
    @Schema(requiredMode = REQUIRED, defaultValue = "LOCAL")
    private @Nullable AttachmentDriverType type;
    private @Nullable String name;
    @JsonProperty("mount_name")
    private @Nullable String mountName;
    /**
     * driver remote relative path or sub dir id, null or empty is root dir.
     */
    @JsonProperty("remote_path")
    private @Nullable String remotePath;
    private @Nullable Long order;
    private @Nullable String comment;
    @JsonProperty("refresh_token")
    private @Nullable String refreshToken;
    @JsonProperty("access_token")
    private @Nullable String accessToken;
    @JsonProperty("expire_time")
    private @Nullable LocalDateTime expireTime;
    @JsonProperty("list_page_size")
    private @Nullable Long listPageSize;
    @JsonProperty("root_dir_id")
    private @Nullable String rootDirId;
    /**
     * api request limit r/1s, default 0.1 r/1s.
     */
    @JsonProperty("request_limit")
    private @Nullable Double requestLimit;

    @JsonProperty("user_id")
    private @Nullable UUID userId;
    @JsonProperty("user_name")
    private @Nullable String username;
    private @Nullable String avatar;
    @JsonProperty("space_total")
    private @Nullable Long spaceTotal;
    @JsonProperty("space_use")
    private @Nullable Long spaceUse;
}
