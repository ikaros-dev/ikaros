package run.ikaros.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import run.ikaros.server.core.service.NotifyService;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.exceptions.RuntimeIkarosException;
import run.ikaros.server.model.dto.OptionNotifyDTO;
import run.ikaros.server.model.request.NotifyMailTestRequest;
import run.ikaros.server.utils.AssertUtils;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService, InitializingBean {

    private final OptionService optionService;
    private JavaMailSenderImpl mailSender;

    public NotifyServiceImpl(OptionService optionService) {
        this.optionService = optionService;
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

    @Override
    public void afterPropertiesSet() throws Exception {
        OptionNotifyDTO optionNotifyDTO = optionService.getOptionNotifyDTO();
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
