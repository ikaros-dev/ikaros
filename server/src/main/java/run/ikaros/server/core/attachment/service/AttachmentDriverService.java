package run.ikaros.server.core.attachment.service;

import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentDriver;

public interface AttachmentDriverService {

    Mono<AttachmentDriver> save(AttachmentDriver driver);

    Mono<Void> removeById(Long id);

    Mono<Void> removeByTypeAndName(String type, String name);

    Mono<AttachmentDriver> findById(Long id);

    Mono<AttachmentDriver> findByTypeAndName(String type, String name);

    Mono<Void> enable(Long driverId);

    Mono<Void> disable(Long driverId);
}
