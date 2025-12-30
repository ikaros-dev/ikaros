package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import run.ikaros.api.store.enums.AttachmentDriverType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "attachment_driver")
public class AttachmentDriverEntity {
    @Id
    private UUID id;
    /**
     * enable current attachment driver.
     */
    private boolean enable;
    /**
     * attachment driver type, such as LOCAL .......
     */
    @Column("d_type")
    private AttachmentDriverType type;
    @Column("d_name")
    private String name;
    @Column("mount_name")
    private String mountName;
    /**
     * driver remote relative path or sub dir id, null or empty is root dir.
     */
    @Column("remote_path")
    private String remotePath;
    @Column("d_order")
    private Long order;
    @Column("d_comment")
    private String comment;
    @Column("refresh_token")
    private String refreshToken;
    @Column("access_token")
    private String accessToken;
    @Column("expire_time")
    private LocalDateTime expireTime;
    @Column("list_page_size")
    private Long listPageSize;
    @Column("root_dir_id")
    private String rootDirId;
    /**
     * api request limit r/1s, default 0.1 r/1s.
     */
    @Column("request_limit")
    private Double requestLimit;

    @Column("user_id")
    private String userId;
    @Column("user_name")
    private String username;
    private String avatar;
    @Column("space_total")
    private Long spaceTotal;
    @Column("space_use")
    private Long spaceUse;
}
