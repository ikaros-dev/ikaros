package run.ikaros.server.core.attachment.service;

import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentDriver;

public interface AttachmentDriverService {

    Mono<AttachmentDriver> save(AttachmentDriver driver);

}
