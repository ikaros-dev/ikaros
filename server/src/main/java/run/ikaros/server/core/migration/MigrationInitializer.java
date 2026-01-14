package run.ikaros.server.core.migration;

import static io.r2dbc.spi.ConnectionFactoryOptions.PASSWORD;
import static io.r2dbc.spi.ConnectionFactoryOptions.USER;
import static run.ikaros.api.core.attachment.AttachmentConst.COVER_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.DOWNLOAD_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.ROOT_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.V_COVER_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.V_DOWNLOAD_DIRECTORY_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.V_ROOT_DIRECTORY_PARENT_ID;
import static run.ikaros.api.core.attachment.AttachmentConst.V_ROOT_DIRECTORY_UUID;
import static run.ikaros.api.infra.utils.StringUtils.snakeToCamel;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import jakarta.annotation.Nonnull;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.mapping.RelationalMappingContext;
import org.springframework.data.relational.core.mapping.RelationalPersistentEntity;
import org.springframework.data.relational.core.mapping.RelationalPersistentProperty;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.data.relational.core.sql.IdentifierProcessing;
import org.springframework.r2dbc.connection.init.ResourceDatabasePopulator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import run.ikaros.api.infra.utils.FileUtils;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.infra.utils.JsonUtils;
import run.ikaros.server.infra.utils.PathResourceUtils;
import run.ikaros.server.infra.utils.RandomUtils;
import run.ikaros.server.store.entity.AttachmentEntity;

@Slf4j
@Component
@ConditionalOnProperty(name = "ikaros.migration.enable", havingValue = "true")
public class MigrationInitializer {
    private final MigrationProperties migrationProperties;
    private final R2dbcEntityTemplate template;
    private final RelationalMappingContext mappingContext;

    private static Map<String, String> rkTableNameMap = new HashMap<>();
    private static Set<String> uuidType = new HashSet<>();

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
        for (String key : rkTableNameMap.keySet()) {
            if (key.startsWith("reference_id")) {
                continue;
            }
            uuidType.add(key);
        }
        uuidType.add("reference_id");
        uuidType.add("parent_id");
    }

    /**
     * Construct.
     */
    public MigrationInitializer(MigrationProperties migrationProperties,
                                R2dbcEntityTemplate template,
                                RelationalMappingContext mappingContext) {
        this.migrationProperties = migrationProperties;
        this.template = template;
        this.mappingContext = mappingContext;
    }

    @EventListener(ApplicationReadyEvent.class)
    private Mono<Void> doMigration(ApplicationReadyEvent event) {
        log.info("Start migration database table records to new database...");
        ConnectionFactoryOptions baseOptions =
            ConnectionFactoryOptions.parse(migrationProperties.getR2dbc().getUrl());

        ConnectionFactoryOptions finalOptions = baseOptions.mutate()
            .option(USER, migrationProperties.getR2dbc().getUsername())
            .option(PASSWORD, migrationProperties.getR2dbc().getPassword())
            .build();

        // Creates a ConnectionPool wrapping an underlying ConnectionFactory
        ConnectionFactory pooledConnectionFactory = ConnectionFactories.get(finalOptions);

        // Create database client
        DatabaseClient targetClient = DatabaseClient.create(pooledConnectionFactory);

        return updateUuidColumnValueForAllTableIfNotExists()
            .then(Mono.defer(() -> createNewTableInTargetDatabase(targetClient)))
            .then(Mono.defer(() -> migrationWithNameIdUuidMap(targetClient)));
    }

    @Nonnull
    private Flux<String> fetchTableNames() {
        // 查询所有的表ID，每张表生成一个Map<ID, UUID>在内存中，
        // 其中附件表ID=0时设置成AttachmentConst里的RootUUID
        // 最后统合内存中的ID=>UUID为 Map<TabName, Map<ID, UUID>>
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
        return template.getDatabaseClient().sql(sql)
            .fetch()
            .all()
            .index()
            .map(t -> {
                Long index = t.getT1();
                Object o = t.getT2().get(tableNameKey);
                return String.valueOf(o);
            });
    }

    @Nonnull
    private Flux<Object> fetchTableIds(String tableName) {
        if ("flyway_schema_history".equalsIgnoreCase(tableName)) {
            return Flux.empty();
        }
        return template.getDatabaseClient()
            .sql("select id from " + tableName + ";")
            .fetch()
            .all()
            .index()
            .map(tuple2 -> tuple2.getT2().get("id"));
    }

    @Nonnull
    private Mono<Map<String, Map<String, String>>> getDbnameIdUuidMap() {
        return fetchTableNames()
            .flatMapSequential(tableName ->
                fetchTableIds(tableName)
                    .collectList()
                    .map(ids -> {
                        Map<String, String> idUuidMap = new HashMap<>(ids.size());
                        for (Object id : ids) {
                            idUuidMap.putIfAbsent(String.valueOf(id), UuidV7Utils.generate());
                        }
                        return idUuidMap;
                    })
                    .map(idUuidMap -> Tuples.of(tableName, idUuidMap)))
            .collectList()
            .map(tuple2s -> {
                Map<String, Map<String, String>> nameIdUuidMaps = new HashMap<>(tuple2s.size());
                for (Tuple2<String, Map<String, String>> tuple2 : tuple2s) {
                    nameIdUuidMaps.putIfAbsent(tuple2.getT1(), tuple2.getT2());
                }
                return nameIdUuidMaps;
            });
    }

    /**
     * 根据表名获取类.
     */
    private Class<?> getEntityClassByTableName(String tableName) {
        return mappingContext.getPersistentEntities().stream()
            .filter(e -> tableName.equalsIgnoreCase(e.getTableName().getReference()))
            .map(PersistentEntity::getType)
            .findFirst()
            .orElseThrow();
    }

    private Mono<Void> updateUuidColumnValueForAllTableIfNotExists() {
        return template.update(AttachmentEntity.class)
            .matching(Query.query(Criteria.empty()
                .and("id").is(ROOT_DIRECTORY_ID)
                .and("uuid").isNull()))
            .apply(Update.update("uuid", V_ROOT_DIRECTORY_UUID))
            .then(template.update(AttachmentEntity.class)
                .matching(Query.query(Criteria.empty()
                    .and("id").is(COVER_DIRECTORY_ID)
                    .and("uuid").isNull()))
                .apply(Update.update("uuid", V_COVER_DIRECTORY_ID)))
            .then(template.update(AttachmentEntity.class)
                .matching(Query.query(Criteria.empty()
                    .and("id").is(DOWNLOAD_DIRECTORY_ID)
                    .and("uuid").isNull()))
                .apply(Update.update("uuid", V_DOWNLOAD_DIRECTORY_ID)))
            .thenMany(Flux.defer(this::fetchTableNames))
            .parallel(4)
            .flatMap(tableName -> {
                return fetchTableIds(tableName)
                    .parallel(10)
                    .flatMap(id -> template.update(getEntityClassByTableName(tableName))
                        .matching(Query.query(Criteria.empty()
                            .and("id").is(id)
                            .and("uuid").isNull()))
                        .apply(Update.update("uuid", UuidV7Utils.generate()))
                        .then())
                    .runOn(Schedulers.boundedElastic());
            })
            .runOn(Schedulers.boundedElastic())
            .then();
    }

    private Mono<Void> executeSqlScript(ConnectionFactory connectionFactory,
                                        ClassPathResource classPathResource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator(
            classPathResource
        );
        return populator.populate(connectionFactory)
            .doOnSuccess(unused -> log.info("Execute sql script from: {}",
                classPathResource.getPath()));
    }

    private Mono<Void> executeSqlScript(ConnectionFactory connectionFactory,
                                        String classPathRelativePath) {
        return executeSqlScript(connectionFactory, new ClassPathResource(classPathRelativePath));
    }

    /**
     * 创建新的表结构, 需要适配v1.1.x的db-migration
     * <ol>
     *     <li>创建表 migrations </li>
     *     <li>创建其它表时同时写纪录到 migrations</li>
     * </ol>
     * .
     *
     */
    private Mono<Void> createNewTableInTargetDatabase(DatabaseClient targetClient) {
        List<Resource> resources = new ArrayList<>();
        try {
            Resource[] resources2 = PathResourceUtils.getResources("db/migration");
            resources.addAll(Arrays.asList(resources2));
        } catch (IOException e) {
            log.warn("Get resource fail in db/migration.", e);
        }
        resources.sort((o1, o2) -> {
            String filename1 = o1.getFilename();
            if (filename1 == null || "".equalsIgnoreCase(filename1)
                || !filename1.startsWith("V")
                || (!filename1.endsWith("sql") && !filename1.endsWith("SQL"))
                || !filename1.contains("__")
            ) {
                return 0;
            }
            filename1 = filename1.substring(1, filename1.indexOf("__"));
            Long l1 = Long.valueOf(filename1);
            String filename2 = o2.getFilename();
            if (filename2 == null || "".equalsIgnoreCase(filename2)
                || !filename2.startsWith("V")
                || (!filename2.endsWith("sql") && !filename2.endsWith("SQL"))
                || !filename2.contains("__")
            ) {
                return 0;
            }
            filename2 = filename2.substring(1, filename2.indexOf("__"));
            Long l2 = Long.valueOf(filename2);
            return Math.toIntExact(l1 - l2);
        });
        return executeSqlScript(targetClient.getConnectionFactory(),
            "db/v1_0_9/V202601141133__DDL_MIGRATION.sql")
            .thenMany(Flux.defer(() -> Flux.fromStream(resources.stream())))
            .flatMapSequential(resource -> {
                String filename = resource.getFilename();
                if (filename == null || "".equalsIgnoreCase(filename)
                    || !filename.startsWith("V")
                    || (!filename.endsWith("sql") && !filename.endsWith("SQL"))
                    || !filename.contains("__")
                ) {
                    return Mono.empty();
                }
                filename = filename.substring(1);
                filename = FileUtils.parseFileNameWithoutPostfix(filename);
                String[] split = filename.split("__");
                if (split.length != 2) {
                    return Mono.empty();
                }
                String id = split[0];
                String desc = split[1].replace("_", " ");
                return executeSqlScript(targetClient.getConnectionFactory(),
                    "db/migration/" + resource.getFilename())
                    .then(Mono.defer(
                        () -> targetClient.sql("insert into migrations(id, description)"
                                + "values (:id, :description)"
                                + "ON CONFLICT (id) DO NOTHING;")
                            .bind("id", Long.valueOf(id))
                            .bind("description", desc)
                            .fetch()
                            .rowsUpdated()
                            .doOnSuccess(count -> {
                                if (count > 0) {
                                    log.info("Insert into migrations record"
                                        + " for id={}, and description={}", id, desc);
                                }
                            })
                            .then()));
            })
            .then();
    }

    private Mono<Void> migrationWithNameIdUuidMap(DatabaseClient targetClient) {
        return fetchTableNames()
            .flatMapSequential(tabName -> fetchTableIds(tabName)
                .flatMapSequential(id -> template.getDatabaseClient()
                    .sql("select * from " + tabName + " where id=:id;")
                    .bind("id", id)
                    .fetch()
                    .one()
                    .map(this::replaceCanModifyMap)
                    .map(this::replaceIdValueFromUuid)
                    .flatMap(this::replaceRkIdValueFromMasterTableIdAndUuid)
                    .flatMap(this::replaceReferenceIdRkIdValueFromMasterTableIdAndUuid)
                    .flatMap(recoreMap -> replaceSelfParentIdFromSourceUuid(tabName, recoreMap))
                    .flatMap(recordMap ->
                        saveRecordToNewDatabase(tabName, recordMap, targetClient))
                )

            ).then();
    }

    private Map<String, Object> replaceCanModifyMap(Map<String, Object> recordMap) {
        Map<String, Object> newMap = new HashMap<>(recordMap.size());
        newMap.putAll(recordMap);
        return newMap;
    }

    private Map<String, Object> replaceIdValueFromUuid(
        Map<String, Object> recordMap
    ) {
        recordMap.put("id", recordMap.remove("uuid"));
        return recordMap;
    }

    private Map<String, Object> copyMap(Map<String, Object> from) {
        Map<String, Object> to = new HashMap<>(from.size());
        to.putAll(from);
        return to;
    }

    private Mono<Map<String, Object>> replaceRkIdValueFromMasterTableIdAndUuid(
        Map<String, Object> recordMap
    ) {
        return Flux.fromStream(copyMap(recordMap).keySet().stream())
            .filter(name -> rkTableNameMap.containsKey(name))
            .filter(name -> recordMap.get(name) != null)
            .flatMapSequential(name -> template.getDatabaseClient()
                .sql("select uuid form " + rkTableNameMap.get(name) + " where id=:id")
                .bind("id", recordMap.get(name))
                .fetch()
                .one()
                .map(recordMap2 -> recordMap2.get("uuid"))
                .map(uuid -> recordMap.put(name, uuid))
                .then()
            )
            .then(Mono.defer(() -> Mono.just(recordMap)));
    }

    private Mono<Map<String, Object>> replaceReferenceIdRkIdValueFromMasterTableIdAndUuid(
        Map<String, Object> recordMap
    ) {
        Map<String, Object> onlyReadMap = copyMap(recordMap);
        if (!onlyReadMap.containsKey("reference_id")) {
            return Mono.just(recordMap);
        }
        if (!onlyReadMap.containsKey("type")) {
            log.warn("Migration table 'attachment_reference' has issue for record: {}",
                JsonUtils.obj2Json(recordMap));
            return Mono.just(recordMap);
        }
        String rkKey = "reference_id@type@" + onlyReadMap.get("type").toString().toUpperCase();

        return template.getDatabaseClient()
            .sql("select uuid form " + rkTableNameMap.get(rkKey) + " where id=:id")
            .bind("id", recordMap.get("reference_id"))
            .fetch()
            .one()
            .map(recordMap2 -> recordMap2.get("uuid"))
            .map(uuid -> recordMap.put("reference_id", uuid))
            .then(Mono.defer(() -> Mono.just(recordMap)));
    }

    private Mono<Map<String, Object>> replaceSelfParentIdFromSourceUuid(
        String tableName, Map<String, Object> recordMap
    ) {
        Map<String, Object> onlyReadMap = copyMap(recordMap);
        if (!onlyReadMap.containsKey("parent_id")) {
            return Mono.just(recordMap);
        }

        Object id = onlyReadMap.get("parent_id");

        if ("attachment".equalsIgnoreCase(tableName)
            && id instanceof String ids
            && ("-1".equalsIgnoreCase(ids)
            || "0".equalsIgnoreCase(ids))) {
            recordMap.put("parent_id", V_ROOT_DIRECTORY_PARENT_ID);
            return Mono.just(recordMap);
        }

        return template.getDatabaseClient()
            .sql("select uuid form " + tableName + " where id=:id")
            .bind("id", id)
            .fetch()
            .one()
            .map(recordMap2 -> recordMap2.get("uuid"))
            .map(uuid -> recordMap.put("parent_id", uuid))
            .then()
            .then(Mono.defer(() -> Mono.just(recordMap)));
    }

    private Class<?> getFieldTypeByColumnName(Class<?> entityClass, String columnName) {
        // 1. 获取实体的元数据
        RelationalPersistentEntity<?> entity =
            mappingContext.getRequiredPersistentEntity(entityClass);

        // 2. 遍历所有属性，比对数据库列名
        for (RelationalPersistentProperty property : entity) {
            // getColumnName() 会处理 @Column 注解定义的别名或默认转换逻辑
            if (property.getColumnName().toSql(IdentifierProcessing.NONE)
                .equalsIgnoreCase(columnName)) {
                return property.getType();
            }
        }

        // 3. 如果没找到，尝试将下划线转驼峰后直接按字段名查找（兜底方案）
        RelationalPersistentProperty persistentProperty =
            entity.getPersistentProperty(snakeToCamel(columnName));
        return persistentProperty == null ? Object.class :
            persistentProperty.getType(); // 彻底找不到返回 Object.class
    }


    private Mono<Long> insertMapIfAbsent(DatabaseClient client, String tableName,
                                         Map<String, Object> data) {
        // 1. 提取所有列名
        String columns = String.join(", ", data.keySet());

        // 2. 构建占位符 (例如 :id, :name, :data)
        String placeholders = data.keySet().stream()
            .map(key -> ":" + key)
            .collect(Collectors.joining(", "));

        // 3. 构建 ON CONFLICT 语句 (假设主键名为 id)
        // 如果主键名不是 id，请替换为实际的唯一约束列名
        String sql = String.format(
            "INSERT INTO %s (%s) VALUES (%s) ON CONFLICT (id) DO NOTHING",
            tableName, columns, placeholders
        );

        // 4. 创建执行对象
        DatabaseClient.GenericExecuteSpec spec = client.sql(sql);

        Class<?> cls = getEntityClassByTableName(tableName);

        // 5. 循环 bind 数据
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            if (entry.getValue() == null) {
                // 注意：R2DBC 绑定 null 需要指定类型，这里简单处理，实际可根据 key 获取实体类型
                Class<?> fieldCls;
                if (uuidType.contains(entry.getKey())) {
                    fieldCls = UUID.class;
                } else {
                    fieldCls = getFieldTypeByColumnName(cls, entry.getKey());
                }
                spec = spec.bindNull(entry.getKey(), fieldCls);
            } else {
                spec = spec.bind(entry.getKey(), entry.getValue());
            }
        }

        return spec.fetch().rowsUpdated(); // 返回 1 表示插入成功，0 表示已存在跳过
    }

    private Mono<Void> saveRecordToNewDatabase(String tabName,
                                               Map<String, Object> recordMap,
                                               DatabaseClient targetClient) {
        return insertMapIfAbsent(targetClient, tabName, recordMap)
            .onErrorResume(DuplicateKeyException.class,
                e -> {
                    if ("ikuser".equalsIgnoreCase(tabName)) {
                        recordMap.put("username", recordMap.get("username")
                            + String.valueOf(RandomUtils.getRandom().nextInt(1, 100)));
                        return insertMapIfAbsent(targetClient, tabName, recordMap);
                    }
                    return Mono.error(e);
                })
            .doOnSuccess(count -> {
                if (count > 0) {
                    log.info("Update table={} from record={}", tabName,
                        JsonUtils.obj2Json(recordMap));
                }
            })
            .doOnError(throwable ->
                log.error("Insert fail for table={} from record={}", tabName,
                    JsonUtils.obj2Json(recordMap), throwable))
            .then();

    }
}
