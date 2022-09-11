package cn.liguohao.ikaros.model;

/**
 * @author guohao
 * @date 2022/09/08
 */
public class AuthUser {
    private String username;
    private String password;
    private String role;
    private String token;

    private Boolean rememberMe = false;

    public String getUsername() {
        return username;
    }

    public AuthUser setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public AuthUser setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getRole() {
        return role;
    }

    public AuthUser setRole(String role) {
        this.role = role;
        return this;
    }

    public String getToken() {
        return token;
    }

    public AuthUser setToken(String token) {
        this.token = token;
        return this;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public AuthUser setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
        return this;
    }
}
