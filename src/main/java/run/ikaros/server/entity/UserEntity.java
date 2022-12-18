package run.ikaros.server.entity;


import jakarta.persistence.Column;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import run.ikaros.server.constants.UserConst;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import static jakarta.persistence.FetchType.LAZY;

/**
 * 用户表
 *
 * @author liguohao
 */
@Data
@Entity
@Table(name = "ikuser")
@EqualsAndHashCode(callSuper = true)
public class UserEntity extends BaseEntity {


    /**
     * 用户名
     */
    private String username;

    /**
     * 用户密码已MD5加密
     */
    private String password;

    /**
     * 用户昵称
     */
    private String nickname;

    /**
     * 个人介绍
     */
    @Basic(fetch = LAZY)
    @Column(columnDefinition = "mediumblob")
    private String introduce;

    /**
     * 手机号
     */
    private String telephone;

    /**
     * 主页链接
     */
    private String site;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 是否启用，默认启用
     */
    private Boolean enable = true;

    /**
     * 是否未锁上，默认未锁上。
     */
    private Boolean nonLocked = true;

    private String avatar;
    private String lastLoginIp;
    private Long lastLoginTime;

}
