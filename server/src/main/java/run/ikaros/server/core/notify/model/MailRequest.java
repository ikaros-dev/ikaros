package run.ikaros.server.core.notify.model;

import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class MailRequest {
    private @Nullable String address;
    private @Nullable String title;
    private @Nullable String content;
}
