package run.ikaros.server.model.request;

import lombok.Data;

@Data
public class NotifyMailTestRequest {
    private String address;
    private String subject;
    private String content;
}
