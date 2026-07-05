package run.ikaros.server.store.entity;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Table;

/**
 * 用户TOTP二步验证配置实体.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@Table(name = "ikuser_totp")
@EqualsAndHashCode(callSuper = true)
public class UserTotpEntity extends BaseEntity {

    /**
     * 用户ID.
     */
    private UUID userId;

    /**
     * TOTP密钥(Base32).
     */
    private String secret;

    /**
     * 是否已启用二步验证.
     */
    private Boolean enabled;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
