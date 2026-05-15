package run.ikaros.server.core.notify;

import org.thymeleaf.context.Context;
import reactor.core.publisher.Mono;

public interface NotifyService {
    Mono<Void> sendMail(String title, String context);

    Mono<Void> send(String title, String template, Context context);

    Mono<Void> testMail();
}
