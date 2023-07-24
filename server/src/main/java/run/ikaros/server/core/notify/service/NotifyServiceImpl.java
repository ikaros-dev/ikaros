package run.ikaros.server.core.notify.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.notify.NotifyService;

@Slf4j
@Service
public class NotifyServiceImpl implements NotifyService {
    @Override
    public Mono<Void> sendMail(String title, String context) {
        return null;
    }
}
