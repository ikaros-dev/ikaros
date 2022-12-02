package run.ikaros.server.model.dto;

import lombok.Data;

@Data
public class OptionQbittorrentDTO {
    private String urlPrefix;
    private Boolean enableAuth;
    private String username;
    private String password;
}
