package run.ikaros.server.entity;


import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.Type;
import run.ikaros.server.constants.UserConst;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

import static javax.persistence.FetchType.LAZY;

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
    @Lob
    @Basic(fetch = LAZY)
    @Type(type = "org.hibernate.type.TextType")
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
