package cn.liguohao.ikaros.dto;

/**
 * @author guohao
 * @date 2022/09/08
 */
public class UserDto {
    private String username;
    private String password;
    private String role;

    public String getUsername() {
        return username;
    }

    public UserDto setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public UserDto setPassword(String password) {
        this.password = password;
        return this;
    }

    public String getRole() {
        return role;
    }

    public UserDto setRole(String role) {
        this.role = role;
        return this;
    }
}
