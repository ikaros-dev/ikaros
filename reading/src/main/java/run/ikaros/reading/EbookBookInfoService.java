package run.ikaros.reading;

import java.util.UUID;
import reactor.core.publisher.Mono;

public interface EbookBookInfoService {
    Mono<EbookBookInfoView> get(UUID ownerId, UUID importId);
}
