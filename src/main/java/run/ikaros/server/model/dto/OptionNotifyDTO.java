package run.ikaros.server.model.dto;

import lombok.Data;
import run.ikaros.server.enums.MailProtocol;

@Data
public class OptionNotifyDTO {
    private Boolean mailEnable;
    private MailProtocol mailProtocol;
    private String mailSmtpHost;
    private Integer mailSmtpPort;
    private String mailSmtpAccount;
    private String mailSmtpAccountAlias;
    private String mailSmtpPassword;
}
