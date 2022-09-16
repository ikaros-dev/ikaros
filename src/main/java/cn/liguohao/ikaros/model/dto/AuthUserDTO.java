package cn.liguohao.ikaros.model.dto;

/**
 * @author guohao
 * @date 2022/09/08
 */
public class AuthUserDTO {
    private Long id;
    private String username;
    private String password;
    private String role;
    private String token;

    private Boolean rememberMe = false;

    public String getUsername() {
        return username;
    }

    public AuthUserDTO setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public AuthUserDTO setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getRole() {
        return role;
    }

    public AuthUserDTO setRole(String role) {
        this.role = role;
        return this;
    }

    public String getToken() {
        return token;
    }

    public AuthUserDTO setToken(String token) {
        this.token = token;
        return this;
    }

    public Boolean getRememberMe() {
        return rememberMe;
    }

    public AuthUserDTO setRememberMe(Boolean rememberMe) {
        this.rememberMe = rememberMe;
        return this;
    }

    public Long getId() {
        return id;
    }

    public AuthUserDTO setId(Long id) {
        this.id = id;
        return this;
    }
}
