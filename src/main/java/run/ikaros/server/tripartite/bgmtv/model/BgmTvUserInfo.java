package run.ikaros.server.tripartite.bgmtv.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class BgmTvUserInfo {
    private BgmTvImages avatar;
    private String sign;
    private String username;
    private String nickname;
    private Long id;
    @JsonProperty("user_group")
    private Long userGroup;

    public BgmTvImages getAvatar() {
        return avatar;
    }

    public BgmTvUserInfo setAvatar(BgmTvImages avatar) {
        this.avatar = avatar;
        return this;
    }

    public String getSign() {
        return sign;
    }

    public BgmTvUserInfo setSign(String sign) {
        this.sign = sign;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public BgmTvUserInfo setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getNickname() {
        return nickname;
    }

    public BgmTvUserInfo setNickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public Long getId() {
        return id;
    }

    public BgmTvUserInfo setId(Long id) {
        this.id = id;
        return this;
    }

    public Long getUserGroup() {
        return userGroup;
    }

    public BgmTvUserInfo setUserGroup(Long userGroup) {
        this.userGroup = userGroup;
        return this;
    }
}
