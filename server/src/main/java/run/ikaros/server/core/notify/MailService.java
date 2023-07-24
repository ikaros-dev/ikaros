package run.ikaros.server.core.notify;

import jakarta.mail.MessagingException;
import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.notify.model.MailConfig;
import run.ikaros.server.core.notify.model.MailRequest;

public interface MailService {
    MailConfig getMailConfig();

    Mono<Void> updateConfig();

    Mono<Void> send(MailRequest request) throws MessagingException;

    Mono<Void> send(MailRequest request, String template, Context context);
}
