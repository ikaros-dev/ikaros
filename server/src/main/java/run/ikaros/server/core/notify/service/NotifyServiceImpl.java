package run.ikaros.server.core.notify.service;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.notify.MailService;
import run.ikaros.server.core.notify.NotifyService;
import run.ikaros.server.core.notify.model.MailRequest;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {
    private final MailService mailService;

    public NotifyServiceImpl(MailService mailService) {
        this.mailService = mailService;
    }

    @Override
    public Mono<Void> sendMail(String title, String context) {
        Assert.hasText(title, "title is empty");
        Assert.hasText(context, "context is empty");
        String receiveAddress = mailService.getMailConfig().getReceiveAddress();
        Assert.hasText(receiveAddress, "receive address is empty");
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTitle(title);
        mailRequest.setContent(context);
        mailRequest.setAddress(receiveAddress);
        try {
            return mailService.send(mailRequest);
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Mono<Void> testMail() {
        String receiveAddress = mailService.getMailConfig().getReceiveAddress();
        Assert.hasText(receiveAddress, "receive address is empty");
        MailRequest mailRequest = new MailRequest();
        mailRequest.setTitle("Ikaros Mail Notify Test");
        mailRequest.setAddress(receiveAddress);
        final String template = "mail/notify_test";
        return mailService.send(mailRequest, template, new Context());
    }
}
