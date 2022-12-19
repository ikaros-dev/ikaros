package run.ikaros.server.model.request;

import lombok.Data;

@Data
public class TorrentAddRequest {
    private String type;
    private String content;
}
