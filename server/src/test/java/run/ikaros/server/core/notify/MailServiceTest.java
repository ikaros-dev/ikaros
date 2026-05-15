package run.ikaros.server.core.notify;


import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.core.notify.model.MailRequest;
import run.ikaros.server.core.setting.SystemSettingInitListener;

@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class MailServiceTest {

    @Autowired
    MailService mailService;
    @Autowired
    ReactiveCustomClient reactiveCustomClient;

    @Test
    @Disabled
    void send() throws MessagingException {
        ConfigMap configMap = new ConfigMap();
        configMap.setName(SystemSettingInitListener.getConfigMapName());
        configMap.putDataItem("MAIL_ENABLE", "true");
        configMap.putDataItem("MAIL_PROTOCOL", "smtp");
        configMap.putDataItem("MAIL_SMTP_HOST", "smtpdm.aliyun.com");
        configMap.putDataItem("MAIL_SMTP_PORT", "465");
        configMap.putDataItem("MAIL_SMTP_ACCOUNT", System.getenv("IKAROS_MAIL_ACCOUNT"));
        configMap.putDataItem("MAIL_SMTP_PASSWORD", System.getenv("IKAROS_MAIL_PASSWORD"));
        configMap.putDataItem("MAIL_SMTP_ACCOUNT_ALIAS", "IkarosDev");
        configMap.putDataItem("MAIL_RECEIVE_ADDRESS", "git@liguohao.cn");
        reactiveCustomClient.update(configMap).subscribe();
        mailService.updateConfig().subscribe();
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTitle("测试邮件");
        mailRequest.setContent("这是测试邮件的内容。");
        mailService.send(mailRequest).subscribe();
    }
}