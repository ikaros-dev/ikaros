package run.ikaros.server.core.notify.model;

import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class MailConfig {
    private @Nullable Boolean enable;
    private @Nullable MailProtocol protocol;
    private @Nullable String host;
    private @Nullable Integer port;
    private @Nullable String account;
    private @Nullable String password;
    private @Nullable String accountAlias;
    private @Nullable String receiveAddress;
}
