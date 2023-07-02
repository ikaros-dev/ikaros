package run.ikaros.server.store.entity;


import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * user entity.
 *
 * @author liguohao
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ikuser")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {

    @Column("role_id")
    private Long roleId;

    /**
     * username.
     */
    private String username;

    /**
     * password that encrypted by md5.
     */
    private String password;

    /**
     * nickname, can modify.
     */
    private String nickname;

    private String introduce;

    private String telephone;

    private String site;

    private String email;

    private Boolean enable;

    private Boolean nonLocked;

    private String avatar;
    @Column("last_login_ip")
    private String lastLoginIp;
    @Column("last_login_time")
    private LocalDateTime lastLoginTime;

}
