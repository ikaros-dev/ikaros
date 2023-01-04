package run.ikaros.server.store.entity;


import static jakarta.persistence.FetchType.LAZY;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.relational.core.mapping.Table;

/**
 * user entity.
 *
 * @author liguohao
 */
@Data
@Entity
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


    private Boolean enable = true;

    private Boolean nonLocked = true;

    private String avatar;
    private String lastLoginIp;
    private Long lastLoginTime;

}
