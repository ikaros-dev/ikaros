package run.ikaros.server.core.notify;

import reactor.core.publisher.Mono;

public interface NotifyService {
    Mono<Void> sendMail(String title, String context);
}
