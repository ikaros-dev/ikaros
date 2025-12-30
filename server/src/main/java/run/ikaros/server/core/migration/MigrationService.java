package run.ikaros.server.core.migration;

import org.springframework.core.io.buffer.DefaultDataBuffer;
import reactor.core.publisher.Flux;

public interface MigrationService {
    Flux<DefaultDataBuffer> exportDatabaseTable(String name);
}
