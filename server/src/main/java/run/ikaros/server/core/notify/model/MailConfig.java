package run.ikaros.server.core.notify.model;

import lombok.Data;

@Data
public class MailConfig {
    private Boolean enable;
    private MailProtocol protocol;
    private String host;
    private Integer port;
    private String account;
    private String password;
    private String accountAlias;
    private String receiveAddress;
}
