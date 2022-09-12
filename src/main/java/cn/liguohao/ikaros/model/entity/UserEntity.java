package cn.liguohao.ikaros.model.entity;


import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * 用户表
 *
 * @author liguohao
 */
@Entity
@Table(name = "ikuser")
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
    @Column(columnDefinition = "mediumtext")
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


    public String getUsername() {
        return username;
    }

    public UserEntity setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserEntity setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getNickname() {
        return nickname;
    }

    public UserEntity setNickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public String getIntroduce() {
        return introduce;
    }

    public UserEntity setIntroduce(String introduce) {
        this.introduce = introduce;
        return this;
    }

    public String getTelephone() {
        return telephone;
    }

    public UserEntity setTelephone(String telephone) {
        this.telephone = telephone;
        return this;
    }

    public String getSite() {
        return site;
    }

    public UserEntity setSite(String site) {
        this.site = site;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserEntity setEmail(String email) {
        this.email = email;
        return this;
    }

    public Boolean getEnable() {
        return enable;
    }

    public UserEntity setEnable(Boolean enable) {
        this.enable = enable;
        return this;
    }

    public Boolean getNonLocked() {
        return nonLocked;
    }

    public UserEntity setNonLocked(Boolean nonLocked) {
        this.nonLocked = nonLocked;
        return this;
    }
}
