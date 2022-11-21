package run.ikaros.server.model.request;

public class AppInitRequest {
    private String title;
    private String description;
    private String username;
    private String password;

    public String getTitle() {
        return title;
    }

    public AppInitRequest setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public AppInitRequest setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getUsername() {
        return username;
    }

    public AppInitRequest setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getPassword() {
        return password;
    }

    public AppInitRequest setPassword(String password) {
        this.password = password;
        return this;
    }
}
