package run.ikaros.server.core.notify.model;

import lombok.Data;

@Data
public class MailRequest {
    private String address;
    private String title;
    private String content;
}
