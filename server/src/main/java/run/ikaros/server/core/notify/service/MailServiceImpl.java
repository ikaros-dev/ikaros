package run.ikaros.server.core.notify.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.server.core.notify.MailService;
import run.ikaros.server.core.notify.model.MailConfig;
import run.ikaros.server.core.notify.model.MailProtocol;
import run.ikaros.server.core.notify.model.MailRequest;
import run.ikaros.server.core.setting.SystemSettingInitListener;
import run.ikaros.server.custom.event.CustomUpdateEvent;

@Slf4j
@Component
public class MailServiceImpl implements MailService {
    private final ReactiveCustomClient reactiveCustomClient;
    private final TemplateEngine templateEngine;
    private MailConfig mailConfig = new MailConfig();
    private JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

    public MailServiceImpl(ReactiveCustomClient reactiveCustomClient,
                           TemplateEngine templateEngine) {
        this.reactiveCustomClient = reactiveCustomClient;
        this.templateEngine = templateEngine;
    }

    public MailConfig getMailConfig() {
        return mailConfig;
    }


    /**
     * Update mail config.
     */
    @EventListener({ApplicationReadyEvent.class, CustomUpdateEvent.class})
    public Mono<Void> updateConfig() {
        return reactiveCustomClient.findOne(ConfigMap.class,
                SystemSettingInitListener.getConfigMapName())
            .onErrorResume(NotFoundException.class, e -> Mono.empty())
            .map(ConfigMap::getData)
            .map(map -> {
                Object mailEnable = map.get("MAIL_ENABLE");
                if (Objects.nonNull(mailEnable)) {
                    Boolean mailEnableB = false;
                    if (mailEnable instanceof String) {
                        mailEnableB = Boolean.valueOf((String) mailEnable);
                    }
                    if (mailEnable instanceof Boolean) {
                        mailEnableB = (Boolean) mailEnable;
                    }
                    mailConfig.setEnable(mailEnableB);
                }

                String mailProtocol = map.get("MAIL_PROTOCOL");
                if (StringUtils.isNotBlank(mailProtocol)
                    && "smtp".equalsIgnoreCase(mailProtocol)) {
                    mailConfig.setProtocol(MailProtocol.SMTP);
                }
                String mailSmtpHost = map.get("MAIL_SMTP_HOST");
                if (StringUtils.isNotBlank(mailSmtpHost)) {
                    mailConfig.setHost(mailSmtpHost);
                }
                String mailSmtpPort = map.get("MAIL_SMTP_PORT");
                if (StringUtils.isNotBlank(mailSmtpPort)) {
                    mailConfig.setPort(Integer.valueOf(mailSmtpPort));
                }
                String mailSmtpAccount = map.get("MAIL_SMTP_ACCOUNT");
                if (StringUtils.isNotBlank(mailSmtpAccount)) {
                    mailConfig.setAccount(mailSmtpAccount);
                }
                String mailSmtpPassword = map.get("MAIL_SMTP_PASSWORD");
                if (StringUtils.isNotBlank(mailSmtpPassword)) {
                    mailConfig.setPassword(mailSmtpPassword);
                }
                String mailSmtpAccountAlias = map.get("MAIL_SMTP_ACCOUNT_ALIAS");
                if (StringUtils.isNotBlank(mailSmtpAccountAlias)) {
                    mailConfig.setAccountAlias(mailSmtpAccountAlias);
                }
                String mailReceiveAddress = map.get("MAIL_RECEIVE_ADDRESS");
                if (StringUtils.isNotBlank(mailReceiveAddress)) {
                    mailConfig.setReceiveAddress(mailReceiveAddress);
                }
                log.debug("update mail config");
                return mailConfig;
            })
            .map(mc -> {
                mailSender.setHost(mc.getHost());
                mailSender.setPort(mc.getPort());
                mailSender.setProtocol(mc.getProtocol().name().toLowerCase());
                mailSender.setUsername(mc.getAccount());
                mailSender.setPassword(mc.getPassword());
                mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
                Properties properties = new Properties();
                properties.setProperty("mail.smtp.auth", Boolean.TRUE.toString());
                properties.setProperty("mail.smtp.starttls.enable", Boolean.TRUE.toString());
                properties.setProperty("mail.smtp.starttls.required", Boolean.TRUE.toString());
                properties.setProperty("mail.smtp.socketFactory.fallback",
                    Boolean.FALSE.toString());
                properties.setProperty("mail.smtp.socketFactory.port",
                    String.valueOf(mc.getPort()));
                properties.setProperty("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
                mailSender.setJavaMailProperties(properties);
                log.debug("update mail sender.");
                return mc;
            })
            .then();
    }

    @Override
    public Mono<Void> send(MailRequest request) throws MessagingException {
        Assert.notNull(request, "'request' must not null.");
        Assert.hasText(request.getTitle(), "title must has text.");
        Assert.hasText(request.getContent(), "context must has text.");
        String address = StringUtils.isBlank(request.getAddress())
            ? mailConfig.getReceiveAddress() : request.getAddress();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        String fromSb = mailConfig.getAccountAlias()
            + "<"
            + mailConfig.getAccount()
            + ">";
        helper.setFrom(fromSb);
        helper.setTo(address);
        helper.setSubject(request.getTitle());
        helper.setText(request.getContent(), true);

        mailSender.send(message);
        log.info("send mail to {} success, mail title is {}", address, request.getTitle());
        return Mono.empty();
    }

    @Override
    public Mono<Void> send(MailRequest request, String template, Context context) {
        Assert.notNull(request, "'request' must not null.");
        Assert.hasText(request.getTitle(), "title must has text.");
        Assert.hasText(template, "template must has text.");
        Assert.notNull(context, "context must not null.");
        String address = StringUtils.isBlank(request.getAddress())
            ? mailConfig.getReceiveAddress() : request.getAddress();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        String fromSb = mailConfig.getAccountAlias()
            + "<"
            + mailConfig.getAccount()
            + ">";
        try {
            helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromSb);
            helper.setTo(address);
            helper.setSubject(request.getTitle());
            helper.setText(templateEngine.process(template, context), true);

            mailSender.send(message);
            log.info("send mail to {} success, mail subject is {}", address, request.getTitle());
        } catch (MessagingException e) {
            log.error("send mail fail, message exception: ", e);
        } catch (Exception e) {
            log.error("send mail fail, exception: ", e);
        }
        return Mono.empty();
    }

}
