package run.ikaros.api.core.setting.vo;

import lombok.Data;

@Data
public class MailSetting {
    private Boolean enable;
    private MailProtocol protocol;
    private String smtpHost;
    private String smtpPort;
    private String smtpAccount;
    private String smtpPassword;
    private String smtpAccountAlias;
    private String smtpReceiveAddress;
}
