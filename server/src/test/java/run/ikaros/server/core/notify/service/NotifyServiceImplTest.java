package run.ikaros.server.core.notify.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.core.notify.MailService;
import run.ikaros.server.core.notify.model.MailConfig;
import run.ikaros.server.core.notify.model.MailRequest;

@org.jspecify.annotations.NullUnmarked
class NotifyServiceImplTest {
    private MailService mailService;
    private NotifyServiceImpl notifyService;

    @BeforeEach
    void setUp() {
        mailService = Mockito.mock(MailService.class);
        notifyService = new NotifyServiceImpl(mailService);
    }

    @Test
    void sendMail() throws MessagingException {
        MailConfig mailConfig = new MailConfig();
        mailConfig.setReceiveAddress("test@example.com");
        when(mailService.getMailConfig()).thenReturn(mailConfig);
        when(mailService.send(any(MailRequest.class))).thenReturn(Mono.empty());

        StepVerifier
            .create(notifyService.sendMail("Hello", "World"))
            .verifyComplete();
        verify(mailService).send(any(MailRequest.class));
    }

    @Test
    void sendMail_withBlankTitle_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> notifyService.sendMail("", "World"));
    }

    @Test
    void sendMail_withBlankContext_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> notifyService.sendMail("Hello", ""));
    }

    @Test
    void sendMail_whenNoReceiveAddress_throwsException() {
        MailConfig mailConfig = new MailConfig();
        when(mailService.getMailConfig()).thenReturn(mailConfig);

        assertThrows(IllegalArgumentException.class,
            () -> notifyService.sendMail("Hello", "World"));
    }

    @Test
    void send() throws MessagingException {
        MailConfig mailConfig = new MailConfig();
        mailConfig.setReceiveAddress("test@example.com");
        when(mailService.getMailConfig()).thenReturn(mailConfig);
        when(mailService.send(any(MailRequest.class), anyString(), any(Context.class)))
            .thenReturn(Mono.empty());

        StepVerifier
            .create(notifyService.send("Hello", "mail/template", new Context()))
            .verifyComplete();
        verify(mailService).send(any(MailRequest.class), anyString(), any(Context.class));
    }

    @Test
    void send_withBlankTitle_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> notifyService.send("", "mail/template", new Context()));
    }

    @Test
    void send_withNullContext_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> notifyService.send("Hello", "mail/template", null));
    }

    @Test
    void send_whenNoReceiveAddress_throwsException() {
        MailConfig mailConfig = new MailConfig();
        when(mailService.getMailConfig()).thenReturn(mailConfig);

        assertThrows(IllegalArgumentException.class,
            () -> notifyService.send("Hello", "mail/template", new Context()));
    }

    @Test
    void testMail() throws MessagingException {
        MailConfig mailConfig = new MailConfig();
        mailConfig.setReceiveAddress("test@example.com");
        when(mailService.getMailConfig()).thenReturn(mailConfig);
        when(mailService.send(any(MailRequest.class), anyString(), any(Context.class)))
            .thenReturn(Mono.empty());

        StepVerifier
            .create(notifyService.testMail())
            .verifyComplete();
        verify(mailService).send(any(MailRequest.class), anyString(), any(Context.class));
    }

    @Test
    void testMail_whenNoReceiveAddress_throwsException() {
        MailConfig mailConfig = new MailConfig();
        when(mailService.getMailConfig()).thenReturn(mailConfig);

        assertThrows(IllegalArgumentException.class,
            () -> notifyService.testMail());
    }
}
