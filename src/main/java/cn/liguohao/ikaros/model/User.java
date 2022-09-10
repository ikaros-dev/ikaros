package cn.liguohao.ikaros.model;

/**
 * @author guohao
 * @date 2022/09/08
 */
public class User {
    private String username;
    private String password;
    private String role;

    public String getUsername() {
        return username;
    }

    public User setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public User setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getRole() {
        return role;
    }

    public User setRole(String role) {
        this.role = role;
        return this;
    }
}
