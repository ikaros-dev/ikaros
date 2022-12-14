package run.ikaros.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import run.ikaros.server.core.service.NotifyService;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.model.dto.OptionNotifyDTO;
import run.ikaros.server.model.request.NotifyMailTestRequest;
import run.ikaros.server.utils.AssertUtils;

import javax.annotation.Nonnull;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService, InitializingBean {

    private final OptionService optionService;
    private JavaMailSenderImpl mailSender;
    private final TemplateEngine templateEngine;

    public NotifyServiceImpl(OptionService optionService, TemplateEngine templateEngine) {
        this.optionService = optionService;
        this.templateEngine = templateEngine;
    }

    @Override
    public void mailTest(NotifyMailTestRequest notifyMailTestRequest) throws MessagingException {
        AssertUtils.notNull(notifyMailTestRequest, "notifyMailTestRequest");
        AssertUtils.notBlank(notifyMailTestRequest.getAddress(), "notifyMailTestRequest address");
        AssertUtils.notBlank(notifyMailTestRequest.getSubject(), "notifyMailTestRequest subject");
        AssertUtils.notBlank(notifyMailTestRequest.getContent(), "notifyMailTestRequest content");

        if (mailSender == null) {
            throw new RuntimeIkarosException("please config notify mail smtp items");
        }

        OptionNotifyDTO optionNotifyDTO = optionService.getOptionNotifyDTO();
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        String fromSB = optionNotifyDTO.getMailSmtpAccountAlias()
            + "<"
            + optionNotifyDTO.getMailSmtpAccount()
            + ">";
        helper.setFrom(fromSB);
        helper.setTo(notifyMailTestRequest.getAddress());
        helper.setSubject(notifyMailTestRequest.getSubject());
        helper.setText(notifyMailTestRequest.getContent(), true);

        mailSender.send(message);
        log.info("send mail to {} success, mail subject is {}",
            notifyMailTestRequest.getAddress(), notifyMailTestRequest.getSubject());
    }

    private String getMailFrom() {
        OptionNotifyDTO optionNotifyDTO = optionService.getOptionNotifyDTO();
        return optionNotifyDTO.getMailSmtpAccountAlias()
            + "<"
            + optionNotifyDTO.getMailSmtpAccount()
            + ">";
    }

    @Override
    public void sendTemplateMail(@Nonnull String targetMailAddress, @Nonnull String subject,
                                 @Nonnull String template, @Nonnull Context context) {
        AssertUtils.notBlank(targetMailAddress, "targetMailAddress");
        AssertUtils.notBlank(subject, "subject");
        AssertUtils.notBlank(template, "template");
        AssertUtils.notNull(context, "context");
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = null;
        try {
            helper = new MimeMessageHelper(message, true);
            helper.setFrom(getMailFrom());
            helper.setTo(targetMailAddress);
            helper.setSubject(subject);
            helper.setText(templateEngine.process(template, context), true);

            mailSender.send(message);
            log.info("send mail to {} success, mail subject is {}", targetMailAddress, subject);
        } catch (MessagingException e) {
            log.error("send mail fail, message exception: ", e);
        } catch (Exception e) {
            log.error("send mail fail, exception: ", e);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        OptionNotifyDTO optionNotifyDTO = optionService.getOptionNotifyDTO();
        refreshMailSender(optionNotifyDTO);
    }

    public void refreshMailSender(@Nonnull OptionNotifyDTO optionNotifyDTO) {
        AssertUtils.notNull(optionNotifyDTO, "optionNotifyDTO");
        if (optionNotifyDTO.getMailEnable() != null && optionNotifyDTO.getMailEnable()) {
            mailSender = new JavaMailSenderImpl();
            mailSender.setHost(optionNotifyDTO.getMailSmtpHost());
            mailSender.setPort(optionNotifyDTO.getMailSmtpPort());
            mailSender.setProtocol(optionNotifyDTO.getMailProtocol().name().toLowerCase());
            mailSender.setUsername(optionNotifyDTO.getMailSmtpAccount());
            mailSender.setPassword(optionNotifyDTO.getMailSmtpPassword());
            mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());
            Properties properties = new Properties();
            properties.setProperty("mail.smtp.auth", Boolean.TRUE.toString());
            properties.setProperty("mail.smtp.starttls.enable", Boolean.TRUE.toString());
            properties.setProperty("mail.smtp.starttls.required", Boolean.TRUE.toString());
            properties.setProperty("mail.smtp.socketFactory.fallback", Boolean.FALSE.toString());
            properties.setProperty("mail.smtp.socketFactory.port",
                String.valueOf(optionNotifyDTO.getMailSmtpPort()));
            properties.setProperty("mail.smtp.socketFactory.class",
                "javax.net.ssl.SSLSocketFactory");
            mailSender.setJavaMailProperties(properties);
        }
    }
}
