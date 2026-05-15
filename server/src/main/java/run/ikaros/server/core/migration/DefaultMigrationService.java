package run.ikaros.server.core.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.repository.support.Repositories;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;
import run.ikaros.api.constant.FileConst;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.infra.utils.DataBufferFileWriter;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;
import run.ikaros.server.store.entity.RoleEntity;
import run.ikaros.server.store.repository.BaseRepository;

@Slf4j
@Service
public class DefaultMigrationService implements MigrationService {
    private final R2dbcEntityTemplate template;
    private final IkarosProperties ikarosProperties;
    private final DefaultDataBufferFactory dataBufferFactory;
    private final RelationalMappingContext mappingContext;
    private final Repositories repositories;

    private static Map<String, String> rkTableNameMap = new HashMap<>();

    static {
        rkTableNameMap.putAll(Map.of(
            "driver_id", "attachment_driver",
            "user_id", "ikuser",
            "attachment_id", "attachment",
            "reference_id@type@USER_AVATAR", "attachment",
            "reference_id@type@EPISODE", "episode",
            "reference_id@type@SUBJECT", "subject",
            "relation_attachment_id", "attachment",
            "update_uid", "ikuser",
            "create_uid", "ikuser",
            "custom_id", "custom"
        ));
        rkTableNameMap.putAll(Map.of(
            "subject_id", "subject",
            "episode_id", "episode",
            "episode_list_id", "episode_list",
            "role_id", "role",
            "character_id", "character",
            "person_id", "person",
            "authority_id", "authority",
            "relation_subject_id", "subject",
            "master_id", "ikuser"
        ));
    }

    /**
     * Construct.
     */
    public DefaultMigrationService(AttachmentService attachmentService,
                                   R2dbcEntityTemplate template,
                                   IkarosProperties ikarosProperties,
                                   RelationalMappingContext mappingContext,
                                   ListableBeanFactory beanFactory) {
        this.template = template;
        this.ikarosProperties = ikarosProperties;
        this.mappingContext = mappingContext;
        this.dataBufferFactory = new DefaultDataBufferFactory();
        this.repositories = new Repositories(beanFactory);
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
                return Flux.defer(() -> {
                    String csvLine = convertRowToCsv(row) + "\n";
                    if (index == 0) {
                        // 第一行：表头
                        csvLine = String.join(",", row.keySet()) + "\n" + csvLine;
                    }
                    return Flux.just(
                        dataBufferFactory.wrap(csvLine.getBytes(StandardCharsets.UTF_8)));
                });
            })
            .startWith(dataBufferFactory.wrap("".getBytes(StandardCharsets.UTF_8)));
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

    @Override
    public Mono<String> importDatabaseTablesCsv410x(Flux<DataBuffer> dataBuffers) {
        Assert.notNull(dataBuffers, "'dataBuffers' must not null.");
        // 将压缩包写到临时目录
        final Path workDir = ikarosProperties.getWorkDir();
        final Path cacheDir = workDir.resolve(FileConst.DEFAULT_CACHE_DIR_NAME);
        if (Files.notExists(cacheDir)) {
            try {
                Files.createDirectory(cacheDir);
                log.info("create dir for path: {}", cacheDir);
            } catch (IOException e) {
                log.error("create dir fail for path: {}", cacheDir, e);
            }
        }
        final String zipFileNameWithoutPostfix = UuidV7Utils.generate();
        final Path zipFilePath = cacheDir.resolve(zipFileNameWithoutPostfix + ".zip");
        if (Files.notExists(zipFilePath)) {
            try {
                Files.createFile(zipFilePath);
                log.info("create zip file for path: {}", cacheDir);
            } catch (IOException e) {
                log.info("create zip file fail for path: {}", cacheDir, e);
            }
        }
        Mono<Void> zipMono =
            DataBufferFileWriter.writeFluxToFile(dataBuffers,
                zipFilePath.toFile().getAbsolutePath());

        // 解压压缩包
        final Path desDirectoryPath = cacheDir.resolve(zipFileNameWithoutPostfix);
        zipMono = zipMono.then(Mono.defer(() -> unzip(zipFilePath, desDirectoryPath)));

        // 读取所有CSV文件，转化成Map<tabName, List<Map<Long, Entity>>>
        Mono<Map<String, List<Tuple3<String, UUID, String>>>> mapMono
            = zipMono.then(Mono.defer(() -> csvToTuple4(desDirectoryPath)));

        // 按顺序一个个写入数据库
        zipMono = mapMono.flatMap(this::saveCsvMapToDb);

        // 完成则返回
        return zipMono.then(Mono.just("OK"));
    }

    private Mono<Void> unzip(Path zipFilePath, Path desDirectoryPath) {
        try (ZipInputStream zipInputStream =
                 new ZipInputStream(new FileInputStream(zipFilePath.toFile()))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (zipEntry.isDirectory()) {
                    Path unzipFilePath = desDirectoryPath.resolve(zipEntry.getName());

                    Files.createDirectory(unzipFilePath);
                } else {
                    Path unzipFilePath = desDirectoryPath.resolve(zipEntry.getName());
                    // 父目录
                    if (Files.notExists(unzipFilePath.getParent())) {
                        Files.createDirectory(unzipFilePath.getParent());
                    }

                    BufferedOutputStream bufferedOutputStream =
                        new BufferedOutputStream(new FileOutputStream(unzipFilePath.toFile()));
                    byte[] bytes = new byte[1024];
                    int readLen;
                    while ((readLen = zipInputStream.read(bytes)) != -1) {
                        bufferedOutputStream.write(bytes, 0, readLen);
                    }
                    bufferedOutputStream.close();
                }
                zipInputStream.closeEntry();
                zipEntry = zipInputStream.getNextEntry();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return Mono.empty();
    }

    /**
     * 将CSV转化成四个元素的元组
     * Key: TableName/FileName
     * Value: Tuple1=TableId, Tuple2=UUID, Tuple3=JSON
     * .
     */
    private Mono<Map<String, List<Tuple3<String, UUID, String>>>> csvToTuple4(
        Path desDirectoryPath) {
        Map<String, List<Tuple3<String, UUID, String>>> map = new HashMap<>();
        File file = desDirectoryPath.toFile();
        File[] files = file.listFiles();
        if (files == null) {
            return Mono.just(map);
        }
        for (File csvFile : files) {
            try (Reader in = new FileReader(csvFile);
                 // setHeader() 不传参数表示自动使用第一行作为表头
                 CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .get()
                     .parse(in)) {
                for (CSVRecord record : parser) {
                    List<String> headerNames = parser.getHeaderNames();
                    int idIndex = headerNames.indexOf("id");
                    String id = idIndex < 0 ? "" : (record.values())[idIndex];
                    int uuidIndex = headerNames.indexOf("uuid");
                    UUID uuid = UuidV7Utils.fromString(uuidIndex < 0
                        ? ""
                        : (record.values())[uuidIndex]);
                    Map<String, String> entityMap = new HashMap<>();
                    headerNames.forEach(hn -> {
                        int index = headerNames.indexOf(hn);
                        entityMap.putIfAbsent(hn, index < 0
                            ? ""
                            : (record.values())[index]);
                    });
                    String tableName = FileUtils.parseFileNameWithoutPostfix(csvFile.getName());
                    if (map.containsKey(tableName)) {
                        map.get(tableName).add(Tuples.of(id, Objects.requireNonNull(uuid),
                            Objects.requireNonNull(JsonUtils.obj2Json(entityMap))));
                    } else {
                        List<Tuple3<String, UUID, String>> list = new ArrayList<>();
                        list.add(Tuples.of(id, Objects.requireNonNull(uuid),
                            Objects.requireNonNull(JsonUtils.obj2Json(entityMap))));
                        map.put(tableName, list);
                    }
                }
            } catch (IOException | NullPointerException e) {
                log.error("read csv file fail for file={}", csvFile.getAbsolutePath(), e);
            }
        }
        return Mono.just(map);
    }

    @SuppressWarnings("unchecked")
    private Mono<Void> saveCsvMapToDb(Map<String, List<Tuple3<String, UUID, String>>> map) {
        Map<String, Class<?>> nameClsMap = new HashMap<>();
        for (RelationalPersistentEntity<?> persistentEntity :
            mappingContext.getPersistentEntities()) {
            nameClsMap.putIfAbsent(persistentEntity.getTableName().getReference(),
                persistentEntity.getType());
        }
        Map<String, Map<String, UUID>> tableNameIdUuidMap = new HashMap<>();
        for (Map.Entry<String, List<Tuple3<String, UUID, String>>> entry : map.entrySet()) {
            String tabName = entry.getKey();
            List<Tuple3<String, UUID, String>> tuple3s = entry.getValue();
            for (Tuple3<String, UUID, String> tuple3 : tuple3s) {
                if (tableNameIdUuidMap.containsKey(tabName)) {
                    tableNameIdUuidMap.get(tabName).putIfAbsent(tuple3.getT1(), tuple3.getT2());
                } else {
                    Map<String, UUID> map1 = new HashMap<>();
                    map1.putIfAbsent(tuple3.getT1(), tuple3.getT2());
                    tableNameIdUuidMap.putIfAbsent(tabName, map1);
                }
            }
        }

        Map<Class<?>, List<String>> clsEntityJsonMap = new HashMap<>();
        for (Map.Entry<String, List<Tuple3<String, UUID, String>>> entry : map.entrySet()) {
            String tabName = entry.getKey();
            Class<?> cls = nameClsMap.get(tabName);
            if (cls == null) {
                continue;
            }

            List<Tuple3<String, UUID, String>> tuple3s = entry.getValue();
            Map<String, String> attIdUuidMap = new HashMap<>();
            Map<String, String> roleIdUuidMap = new HashMap<>();
            for (Tuple3<String, UUID, String> tuple3 : tuple3s) {
                Map<String, String> entityJsonMap =
                    JsonUtils.json2obj(tuple3.getT3(), HashMap.class);
                if (entityJsonMap == null) {
                    log.error("Convert json to map fail for tabName:{} and json: {}",
                        tabName, tuple3.getT3());
                    continue;
                }
                if ("attachment_reference".equalsIgnoreCase(tabName)) {
                    String key = "reference_id@type@" + entityJsonMap.get("type");
                    String replaceTableName = rkTableNameMap.get(key);
                    String referenceId = entityJsonMap.get("reference_id");
                    UUID uuid = tableNameIdUuidMap.get(replaceTableName)
                        .getOrDefault(referenceId, null);
                    entityJsonMap.put("reference_id", uuid == null ? null : uuid.toString());
                }

                Map<String, String> entityJsonCloneMap = new HashMap<>(entityJsonMap.size());
                entityJsonCloneMap.putAll(entityJsonMap);
                for (Map.Entry<String, String> e2 : entityJsonCloneMap.entrySet()) {
                    String key = e2.getKey();
                    if (!rkTableNameMap.containsKey(key)) {
                        continue;
                    }
                    String replaceTableName = rkTableNameMap.get(key);
                    String replaceTableId = e2.getValue();
                    UUID uuid = tableNameIdUuidMap.get(replaceTableName)
                        .getOrDefault(replaceTableId, null);
                    entityJsonMap.put(key, uuid == null ? null : uuid.toString());
                }

                if ("attachment".equalsIgnoreCase(tabName)) {
                    String id = entityJsonMap.get("id");
                    attIdUuidMap.put(id, entityJsonMap.get("uuid"));
                    if ("0".equalsIgnoreCase(id)) {
                        attIdUuidMap.put(id, AttachmentConst.V_ROOT_DIRECTORY_ID);
                    }
                }
                if ("role".equalsIgnoreCase(tabName)) {
                    roleIdUuidMap.put(entityJsonMap.get("id"), entityJsonMap.get("uuid"));
                }

                // 把ID的值替换成UUID的值
                entityJsonMap.put("id", entityJsonMap.remove("uuid"));

                String resEntityJson = JsonUtils.obj2Json(entityJsonMap);
                if (clsEntityJsonMap.containsKey(cls)) {
                    clsEntityJsonMap.get(cls).add(resEntityJson);
                } else {
                    List<String> jsons = new ArrayList<>();
                    jsons.add(resEntityJson);
                    clsEntityJsonMap.putIfAbsent(cls, jsons);
                }
            }

            // 附件的父辈ID转化成对应的UUID
            if ("attachment".equalsIgnoreCase(tabName)) {
                List<String> attRecordJsons = clsEntityJsonMap.get(AttachmentEntity.class);
                List<String> newAttRecordJsons = new ArrayList<>(attRecordJsons.size());
                for (String attRecordJson : attRecordJsons) {
                    Map<String, String> attRecord =
                        JsonUtils.json2obj(attRecordJson, HashMap.class);
                    if (attRecord == null) {
                        continue;
                    }
                    String parentId = attRecord.get("parent_id");
                    String newParentUuid = attIdUuidMap.getOrDefault(parentId, "");
                    attRecord.put("parent_id", newParentUuid);
                    if ("-1".equalsIgnoreCase(parentId)) {
                        attRecord.put("parent_id", AttachmentConst.V_ROOT_DIRECTORY_PARENT_ID);
                    }
                    if ("0".equalsIgnoreCase(parentId)) {
                        attRecord.put("parent_id", AttachmentConst.V_ROOT_DIRECTORY_ID);
                    }
                    String newAttRecordJson = JsonUtils.obj2Json(attRecord);
                    newAttRecordJsons.add(newAttRecordJson);
                }
                clsEntityJsonMap.put(AttachmentEntity.class, newAttRecordJsons);
            }

            // 角色表的 parent_id => 对应的 UUID
            if ("role".equalsIgnoreCase(tabName)) {
                // roleIdUuidMap.put(entityJsonMap.get("id"), entityJsonMap.get("uuid"));
                List<String> oldRoleJsons = clsEntityJsonMap.get(RoleEntity.class);
                List<String> newRoleJsons = new ArrayList<>(oldRoleJsons.size());
                for (String oldRoleJson : oldRoleJsons) {
                    Map<String, String> roleJsonMap =
                        JsonUtils.json2obj(oldRoleJson, HashMap.class);
                    if (roleJsonMap == null) {
                        continue;
                    }
                    String parentId = roleJsonMap.get("parent_id");
                    String newParentId = roleIdUuidMap.getOrDefault(parentId, "");
                    roleJsonMap.put("parent_id", newParentId);
                    newRoleJsons.add(JsonUtils.obj2Json(roleJsonMap));
                }
                clsEntityJsonMap.put(RoleEntity.class, newRoleJsons);
            }

        }

        Map<Class<?>, List<String>> clsEntityJsonNewMap = new HashMap<>(clsEntityJsonMap.size());
        for (Map.Entry<Class<?>, List<String>> e : clsEntityJsonMap.entrySet()) {
            Class<?> cls = e.getKey();
            RelationalPersistentEntity<?> persistentEntity =
                mappingContext.getPersistentEntity(cls);
            if (persistentEntity == null) {
                log.warn("Current persistentEntity is null for cls: {}", cls);
                continue;
            }

            List<String> jsons = e.getValue();
            List<String> newJsons = new ArrayList<>(jsons.size());
            for (String json : jsons) {
                Map<String, String> columnMap = JsonUtils.json2obj(json, HashMap.class);
                if (columnMap == null) {
                    continue;
                }
                // 数据库列名 => Java字段名称
                Map<String, String> entityJavaNameMap = new HashMap<>();
                for (RelationalPersistentProperty property : persistentEntity) {
                    // 获取 Java 字段名称
                    String javaName = property.getName();
                    // 获取数据库列名 (SqlIdentifier 转为字符串)
                    String columnName = property.getColumnName().getReference();
                    if ("cm_value".equalsIgnoreCase(columnName)) {
                        String v = columnMap.get(columnName);
                        try {
                            JsonUtils.getObjectMapper().readValue(v, CustomMetadataEntity.class);
                        } catch (JsonProcessingException ex) {
                            log.warn("Ignore table custom_metadata column cm_value "
                                + "for illegal json value: {}", v);
                            continue;
                        }
                    }
                    String v = columnMap.get(columnName);
                    entityJavaNameMap.put(javaName, v);
                }
                newJsons.add(JsonUtils.obj2Json(entityJavaNameMap));
            }
            clsEntityJsonNewMap.put(cls, newJsons);
        }

        return Flux.fromStream(clsEntityJsonNewMap.entrySet().stream())
            .flatMap(clsEntityJson -> {
                Class<?> cls = clsEntityJson.getKey();
                List<String> jsons = clsEntityJson.getValue();
                Optional<Object> repositoryOp = repositories.getRepositoryFor(cls);
                if (repositoryOp.isEmpty()) {
                    log.error("Current repository is empty for cls: {}.", cls.getCanonicalName());
                    return Mono.empty();
                }
                Object repositoryObj = repositoryOp.get();
                if (!(repositoryObj instanceof BaseRepository<?>)) {
                    log.error("Current repository is not instanceof BaseRepository for cls: {}",
                        cls.getCanonicalName());
                    return Mono.empty();
                }
                BaseRepository repository = (BaseRepository) repositoryObj;
                return Flux.fromStream(jsons.stream())
                    .parallel(20)
                    .flatMap(
                        json -> Mono.just(Objects.requireNonNull(JsonUtils.json2obj(json, cls)))
                            .onErrorContinue(Exception.class,
                                (t, e) -> log.error(
                                    "Convert json to entity fail for cls={} and json={}",
                                    cls.getCanonicalName(), json, t))
                            .flatMap(o -> repository.insert(o)
                                .onErrorContinue(Exception.class,
                                    (BiConsumer<Throwable, Object>) (throwable, o1) -> log.error(
                                        "Insert entity fail for cls={} and json={}",
                                        cls.getCanonicalName(), json, throwable)))
                    )

                    .runOn(Schedulers.boundedElastic())
                    .then();
            })
            .then();
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
