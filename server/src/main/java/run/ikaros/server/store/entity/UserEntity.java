package run.ikaros.server.store.entity;


import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
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
    private UUID uuid;

}
