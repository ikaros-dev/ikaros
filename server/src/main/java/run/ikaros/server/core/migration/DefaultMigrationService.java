package run.ikaros.server.core.migration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import run.ikaros.api.constant.FileConst;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.infra.utils.DataBufferFileWriter;

@Slf4j
@Service
public class DefaultMigrationService implements MigrationService {
    private final R2dbcEntityTemplate template;
    private final IkarosProperties ikarosProperties;
    private final DefaultDataBufferFactory dataBufferFactory;

    /**
     * Construct.
     */
    public DefaultMigrationService(AttachmentService attachmentService,
                                   R2dbcEntityTemplate template,
                                   IkarosProperties ikarosProperties) {
        this.template = template;
        this.ikarosProperties = ikarosProperties;
        this.dataBufferFactory = new DefaultDataBufferFactory();
    }

    @Override
    public Flux<DefaultDataBuffer> exportDatabaseTable(String name) {
        Assert.hasText(name, "'name' must has text.");
        final boolean isNotAll = StringUtils.isNotBlank(name);
        return template.getDatabaseClient()
            .sql("select * from " + name + ";")
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

    @Override
    public Flux<DataBuffer> exportDatabaseTables() {
        String name = template.getDatabaseClient().getConnectionFactory()
            .getMetadata()
            .getName();
        String sql = "SELECT tablename FROM pg_tables WHERE schemaname = 'public';";
        String tableNameKey;
        if (name.toLowerCase().contains("h2")) {
            sql = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                + "WHERE TABLE_SCHEMA = 'PUBLIC'";
            tableNameKey = "TABLE_NAME";
        } else {
            tableNameKey = "tablename";
        }

        final Path workDir = ikarosProperties.getWorkDir();
        final Path currentDir =
            workDir.resolve(FileConst.DEFAULT_CACHE_DIR_NAME).resolve(UuidV7Utils.generate());
        if (Files.notExists(currentDir)) {
            try {
                Files.createDirectory(currentDir);
                log.info("create dir for path: {}", currentDir);
            } catch (IOException e) {
                log.error("create dir fail for path: {}", currentDir, e);
            }
        }

        return template.getDatabaseClient().sql(sql)
            .fetch()
            .all()
            .index()
            .map(t -> {
                Long index = t.getT1();
                Object o = t.getT2().get(tableNameKey);
                return String.valueOf(o);
            })
            .flatMap(tableName -> {
                Path tableCsvPath = currentDir.resolve(tableName + ".csv");
                if (Files.notExists(tableCsvPath)) {
                    try {
                        Files.createFile(tableCsvPath);
                        log.info("create file for path: {}", tableCsvPath);
                    } catch (IOException e) {
                        log.error("create file fail for path: {}", tableCsvPath, e);
                    }
                }
                String absolutePath = tableCsvPath.toFile().getAbsolutePath();
                return DataBufferFileWriter.writeFluxToFile(
                    exportDatabaseTable(tableName).map(db -> db),
                    absolutePath);
            })
            .thenMany(Flux.defer(() -> DataBufferFileWriter.zipDirectoryToStream(
                currentDir.toFile().getAbsolutePath()
            )));
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
