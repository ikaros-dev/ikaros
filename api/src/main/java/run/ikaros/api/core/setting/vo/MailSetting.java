package run.ikaros.api.core.setting.vo;

import lombok.Data;
import org.jspecify.annotations.Nullable;

@Data
public class MailSetting {
    private @Nullable Boolean enable;
    private @Nullable MailProtocol protocol;
    private @Nullable String smtpHost;
    private @Nullable String smtpPort;
    private @Nullable String smtpAccount;
    private @Nullable String smtpPassword;
    private @Nullable String smtpAccountAlias;
    private @Nullable String smtpReceiveAddress;
}
