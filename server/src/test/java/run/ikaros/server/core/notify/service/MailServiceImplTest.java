package run.ikaros.server.core.notify.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thymeleaf.TemplateEngine;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.server.core.notify.model.MailConfig;
import run.ikaros.server.core.notify.model.MailProtocol;

class MailServiceImplTest {
    private ReactiveCustomClient reactiveCustomClient;
    private TemplateEngine templateEngine;
    private MailServiceImpl mailService;

    private ConfigMap createFullConfigMap(String enable) {
        ConfigMap configMap = new ConfigMap();
        configMap.putDataItem("MAIL_ENABLE", enable);
        configMap.putDataItem("MAIL_PROTOCOL", "smtp");
        configMap.putDataItem("MAIL_SMTP_HOST", "smtp.example.com");
        configMap.putDataItem("MAIL_SMTP_PORT", "587");
        configMap.putDataItem("MAIL_SMTP_ACCOUNT", "user@example.com");
        configMap.putDataItem("MAIL_SMTP_PASSWORD", "secret");
        configMap.putDataItem("MAIL_SMTP_ACCOUNT_ALIAS", "Ikaros");
        configMap.putDataItem("MAIL_RECEIVE_ADDRESS", "admin@example.com");
        return configMap;
    }

    @BeforeEach
    void setUp() {
        reactiveCustomClient = Mockito.mock(ReactiveCustomClient.class);
        templateEngine = Mockito.mock(TemplateEngine.class);
        mailService = new MailServiceImpl(reactiveCustomClient, templateEngine);
    }

    @Test
    void getMailConfig_initialState() {
        MailConfig config = mailService.getMailConfig();
        assertThat(config).isNotNull();
        assertThat(config.getEnable()).isNull();
        assertThat(config.getHost()).isNull();
    }

    @Test
    void updateConfig_whenConfigFound() {
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.just(createFullConfigMap("true")));

        StepVerifier.create(mailService.updateConfig())
            .verifyComplete();

        MailConfig config = mailService.getMailConfig();
        assertThat(config.getEnable()).isTrue();
        assertThat(config.getProtocol()).isEqualTo(MailProtocol.SMTP);
        assertThat(config.getHost()).isEqualTo("smtp.example.com");
        assertThat(config.getPort()).isEqualTo(587);
        assertThat(config.getAccount()).isEqualTo("user@example.com");
        assertThat(config.getPassword()).isEqualTo("secret");
        assertThat(config.getAccountAlias()).isEqualTo("Ikaros");
        assertThat(config.getReceiveAddress()).isEqualTo("admin@example.com");
    }

    @Test
    void updateConfig_whenMailDisabled() {
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.just(createFullConfigMap("false")));

        StepVerifier.create(mailService.updateConfig())
            .verifyComplete();

        MailConfig config = mailService.getMailConfig();
        assertThat(config.getEnable()).isFalse();
        // Other fields should still be set from config map
        assertThat(config.getHost()).isEqualTo("smtp.example.com");
    }

    @Test
    void updateConfig_whenConfigNotFound() {
        when(reactiveCustomClient.findOne(any(), any()))
            .thenReturn(Mono.empty());

        StepVerifier.create(mailService.updateConfig())
            .verifyComplete();

        MailConfig config = mailService.getMailConfig();
        assertThat(config.getEnable()).isNull();
        assertThat(config.getHost()).isNull();
    }
}
