package run.ikaros.server.core.migration;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.core.attachment.service.AttachmentService;

@Slf4j
@Service
public class DefaultMigrationService implements MigrationService {
    private final DatabaseClient databaseClient;
    private final DefaultDataBufferFactory dataBufferFactory;

    /**
     * Construct.
     */
    public DefaultMigrationService(DatabaseClient databaseClient,
                                   AttachmentService attachmentService) {
        this.databaseClient = databaseClient;
        this.dataBufferFactory = new DefaultDataBufferFactory();
    }

    @Override
    public Flux<DefaultDataBuffer> exportDatabaseTable(String name) {
        Assert.hasText(name, "'name' must has text.");
        final boolean isNotAll = StringUtils.isNotBlank(name);
        return databaseClient.sql("select * from " + name + ";")
            .fetch()
            .all()
            .index()
            .flatMapSequential(tuple -> {
                long index = tuple.getT1();
                Map<String, Object> row = new HashMap<>(tuple.getT2());
                row.putIfAbsent("uuid", UuidV7Utils.generate());
                return Flux.defer(() -> {
                    String csvLine;
                    if (index == 0) {
                        // 第一行：表头
                        csvLine = String.join(",", row.keySet()) + "\n";
                    } else {
                        // 数据行
                        csvLine = convertRowToCsv(row) + "\n";
                    }
                    return Flux.just(
                        dataBufferFactory.wrap(csvLine.getBytes(StandardCharsets.UTF_8)));
                });
            })
            .startWith(dataBufferFactory.wrap("".getBytes()));
    }

    private String convertRowToCsv(Map<String, Object> row) {
        return row.values().stream()
            .map(value -> {
                if (value == null) {
                    return "";
                }
                String strValue = value.toString();
                // 处理CSV特殊字符（引号、逗号、换行符）
                if (strValue.contains(",") || strValue.contains("\"") || strValue.contains("\n")) {
                    strValue = "\"" + strValue.replace("\"", "\"\"") + "\"";
                }
                return strValue;
            })
            .collect(Collectors.joining(","));
    }
}
