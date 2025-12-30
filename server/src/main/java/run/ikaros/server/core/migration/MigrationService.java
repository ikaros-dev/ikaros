package run.ikaros.server.core.migration;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import reactor.core.publisher.Flux;

public interface MigrationService {
    Flux<DefaultDataBuffer> exportDatabaseTable(String name);

    /**
     * 压缩包文件，里面有所有的表scv文件.
     *
     * @return 压缩包文件
     */
    Flux<DataBuffer> exportDatabaseTables();
}
