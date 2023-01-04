package run.ikaros.server.store.entity;


import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

/**
 * user entity.
 *
 * @author liguohao
 */
@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ikuser")
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

    @Basic(fetch = LAZY)
    @Column(length = 50000)
    private String introduce;

    private String telephone;

    private String site;

    private String email;

    private Boolean enable;

    private Boolean nonLocked;

    private String avatar;
    private String lastLoginIp;
    private Long lastLoginTime;

}
