package run.ikaros.server.migration;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.TagType;

@Slf4j
@Component
@ConditionalOnProperty(name = "ikaros.migration.enable", havingValue = "true")
public class MigrationInitializer {

    private static final List<String> TABLE_NAMES = List.of(
        "ikuser", "attachment", "attachment_relation", "attachment_reference",
        "authority", "character", "episode", "episode_collection",
        "episode_list", "episode_list_episode", "episode_list_collection",
        "person", "person_character", "role", "role_authority",
        "subject", "subject_character", "subject_collection",
        "subject_person", "subject_relation", "subject_sync",
        "tag", "task", "ikuser_role", "custom", "custom_metadata",
        "attachment_driver"
    );

    /**
     * FK column → referenced table name.
     * Used to replace old int8 id values with migration_uuid strings.
     */
    private static final Map<String, Map<String, String>> FK_COLUMNS = new HashMap<>();

    static {
        // BaseEntity FK columns: create_uid, update_uid → ikuser (所有继承BaseEntity的表)
        String[] baseEntityTables = {
            "ikuser", "character", "authority", "episode", "episode_list",
            "person", "person_character", "role", "subject",
            "subject_character", "subject_relation", "subject_person", "subject_sync"
        };
        for (String table : baseEntityTables) {
            putFk(table, "create_uid", "ikuser");
            putFk(table, "update_uid", "ikuser");
        }

        // Business FK columns
        putFk("attachment", "parent_id", "attachment");
        putFk("attachment", "driver_id", "attachment_driver");
        putFk("attachment_driver", "user_id", "ikuser");
        putFk("attachment_relation", "attachment_id", "attachment");
        putFk("attachment_relation", "relation_attachment_id", "attachment");
        putFk("attachment_reference", "attachment_id", "attachment");
        putFk("attachment_reference", "reference_id", "subject"); // 需要根据 type 动态转表

        putFk("custom_metadata", "custom_id", "custom");

        putFk("ikuser_role", "user_id", "ikuser");
        putFk("ikuser_role", "role_id", "role");
        putFk("role_authority", "role_id", "role");
        putFk("role_authority", "authority_id", "authority");

        putFk("episode", "subject_id", "subject");
        putFk("episode_collection", "user_id", "ikuser");
        putFk("episode_collection", "subject_id", "subject");
        putFk("episode_collection", "episode_id", "episode");

        putFk("subject_collection", "user_id", "ikuser");
        putFk("subject_collection", "subject_id", "subject");

        putFk("subject_relation", "subject_id", "subject");
        putFk("subject_relation", "relation_subject_id", "subject");
        putFk("subject_sync", "subject_id", "subject");

        putFk("tag", "user_id", "ikuser");
        putFk("tag", "master_id", "subject");  // 需要根据 type 动态转表
    }

    private static void putFk(String table, String column, String refTable) {
        FK_COLUMNS.computeIfAbsent(table, k -> new HashMap<>()).put(column, refTable);
    }

    private static final String MIGRATION_SCRIPT_PATTERN = "classpath:db/migration/*.sql";

    private static final Set<String> TIMESTAMP_COLUMNS = Set.of(
        "create_time", "update_time", "expire_time", "air_time"
    );

    private static final Set<String> FLOAT_COLUMNS = Set.of(
        "sequence", "score"
    );

    private static final Set<String> NUMBER_COLUMNS = Set.of(
        "size", "d_order", "list_page_size", "request_limit", "space_total", "space_use",
        "ol_version",
        "progress", "duration", "main_ep_progress", "total", "index"
    );

    private static final Set<String> UUID_COLUMNS = Set.of(
        "create_uid", "update_uid", "parent_id", "driver_id", "attachment_id",
        "relation_attachment_id", "reference_id", "user_id", "subject_id",
        "episode_id", "episode_list_id", "person_id", "character_id",
        "role_id", "authority_id", "relation_subject_id", "master_id", "custom_id", "id"
    );

    private static final Set<String> BOOLEAN_COLUMNS = Set.of(
        "delete_status", "enable", "non_locked", "finish", "nsfw",
        "deleted", "is_private"
    );

    private final R2dbcEntityTemplate oldEntityTemplate;
    private final MigrationR2dbcEntityTemplate newEntityTemplate;
    private final ResourcePatternResolver resourcePatternResolver;

    public MigrationInitializer(R2dbcEntityTemplate oldEntityTemplate,
                                MigrationR2dbcEntityTemplate newEntityTemplate,
                                ResourcePatternResolver resourcePatternResolver) {
        this.oldEntityTemplate = oldEntityTemplate;
        this.newEntityTemplate = newEntityTemplate;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * start migration.
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initialize() {
        log.info("Start database migration ...");

        DatabaseClient client = oldEntityTemplate.getDatabaseClient();

        return Flux.fromIterable(TABLE_NAMES)
            .concatMap(table -> {
                log.info("Generating migration_uuid for table: {}", table);
                return client.sql("SELECT id FROM " + table)
                    .map((row, meta) -> row.get("id", Long.class))
                    .all()
                    .concatMap(id -> {
                        String uuid;
                        if (id == 0L) {
                            uuid = "019b715b-08c7-7509-ab14-2abe47f440f3";
                        } else if (id == 1L) {
                            uuid = "019b715b-5cb5-7407-b571-6688c9e61e5a";
                        } else if (id == 2L) {
                            uuid = "019b715b-97dc-72dd-9e5a-0f714efc89d9";
                        } else {
                            uuid = UuidV7Utils.generate();
                        }
                        return client.sql("UPDATE " + table
                                + " SET migration_uuid = :uuid WHERE id = :id")
                            .bind("uuid", uuid)
                            .bind("id", id)
                            .fetch()
                            .rowsUpdated();
                    })
                    .count()
                    .doOnNext(count -> log.info("Updated {} rows in {}", count, table));
            })
            .then()
            .doOnSuccess(v -> log.info("Database migration UUID generation completed."))
            .then(runMigrationScripts())
            .then(migrateAllData())
            .doOnSuccess(v -> log.info("All data migration completed."));
    }

    private Mono<Void> runMigrationScripts() {
        return Mono.fromCallable(
                () -> resourcePatternResolver.getResources(MIGRATION_SCRIPT_PATTERN))
            .flatMapIterable(resources -> List.of(resources))
            .sort((a, b) -> a.getFilename().compareTo(b.getFilename()))
            .concatMap(resource -> Mono.fromCallable(() ->
                    new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(sql -> {
                    log.info("Executing migration script: {}", resource.getFilename());
                    return newEntityTemplate.getDatabaseClient()
                        .sql(sql)
                        .fetch()
                        .rowsUpdated();
                })
            )
            .then()
            .doOnSuccess(v -> log.info("All migration scripts executed successfully."));
    }

    private Mono<Void> migrateAllData() {
        return loadAllIdMappings()
            .then(Flux.fromIterable(TABLE_NAMES)
                .concatMap(this::migrateTableData)
                .then());
    }

    /**
     * Load id → migration_uuid mapping for every table into a nested map.
     */
    private Mono<Void> loadAllIdMappings() {
        Mono<Void> load = Mono.empty();
        for (String table : TABLE_NAMES) {
            load = load.then(loadIdMapping(table));
        }
        return load;
    }

    private Mono<Void> loadIdMapping(String table) {
        return oldEntityTemplate.getDatabaseClient()
            .sql("SELECT id, migration_uuid FROM " + table)
            .map((row, meta) -> {
                Long id = row.get("id", Long.class);
                String uuid = row.get("migration_uuid", String.class);
                return Map.entry(id, uuid);
            })
            .all()
            .collectMap(Map.Entry::getKey, Map.Entry::getValue)
            .doOnNext(map -> {
                idMappings.put(table, map);
                log.info("Loaded {} id mappings for {}", map.size(), table);
            })
            .then();
    }

    private final Map<String, Map<Long, String>> idMappings = new HashMap<>();

    /**
     * Migrate all rows from old table to new table, replacing id and FK columns with uuids.
     */
    private Mono<Void> migrateTableData(String table) {
        return oldEntityTemplate.getDatabaseClient()
            .sql("SELECT * FROM " + table)
            .fetch()
            .all()
            .index()
            .concatMap(tuple -> {
                Map<String, Object> oldRow = tuple.getT2();
                Map<String, Object> newRow = new HashMap<>(oldRow);

                // Remove migration_uuid - not a column in new tables
                newRow.remove("migration_uuid");
                newRow.remove("uuid");

                // Replace id with old record's migration_uuid
                Long oldId = ((Number) oldRow.get("id")).longValue();
                String uuid = idMappings.get(table).get(oldId);
                newRow.put("id", uuid);

                // Replace FK columns with their referenced table's uuid
                Map<String, String> fkCols = FK_COLUMNS.get(table);
                if (fkCols != null) {
                    for (var entry : fkCols.entrySet()) {
                        String fkCol = entry.getKey();
                        String refTable = entry.getValue();
                        if ("attachment_reference".equalsIgnoreCase(table)
                            && fkCol.contains("reference_id")) {
                            String type = oldRow.get("type").toString();
                            if (AttachmentReferenceType.SUBJECT.name().equalsIgnoreCase(type)) {
                                refTable = "subject";
                            } else if (AttachmentReferenceType.EPISODE.name()
                                .equalsIgnoreCase(type)) {
                                refTable = "episode";
                            } else {
                                refTable = "attachment";
                            }
                        }
                        if ("tag".equalsIgnoreCase(table)
                            && fkCol.contains("master_id")) {
                            String type = oldRow.get("type").toString();
                            if (TagType.SUBJECT.name().equalsIgnoreCase(type)) {
                                refTable = "subject";
                            } else if (TagType.EPISODE.name().equalsIgnoreCase(type)) {
                                refTable = "episode";
                            } else {
                                refTable = "attachment";
                            }
                        }
                        Object fkVal = oldRow.get(fkCol);
                        if (fkVal != null && idMappings.containsKey(refTable)) {
                            Long fkId = ((Number) fkVal).longValue();
                            String fkUuid = idMappings.get(refTable).get(fkId);
                            if (fkUuid != null) {
                                newRow.put(fkCol, fkUuid);
                            }
                        }
                    }
                }

                return insertRow(table, newRow);
            })
            .then()
            .doOnNext(v -> log.info("Migrated data for table: {}", table));
    }

    /**
     * Insert one row into the new database table.
     */
    private Mono<Void> insertRow(String table, Map<String, Object> row) {
        List<String> columns = new ArrayList<>(row.keySet());
        List<String> placeholders = new ArrayList<>();
        for (int i = 0; i < columns.size(); i++) {
            placeholders.add(":p" + i);
        }
        String sql = "INSERT INTO " + table + " ("
            + String.join(", ", columns) + ") VALUES ("
            + String.join(", ", placeholders) + ")";

        DatabaseClient.GenericExecuteSpec spec = newEntityTemplate.getDatabaseClient().sql(sql);
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            Object value = convertType(row.get(column), column, table);
            if (value == null) {
                spec = spec.bindNull("p" + i, convertNullColumnType(column));
            } else {
                spec = spec.bind("p" + i, value);
            }
        }
        return spec.fetch().rowsUpdated().then();
    }

    private Class<?> convertNullColumnType(String column) {

        if (FLOAT_COLUMNS.contains(column)) {
            return Double.class;
        }
        if (NUMBER_COLUMNS.contains(column)) {
            return Long.class;
        }
        if (UUID_COLUMNS.contains(column)) {
            return UUID.class;
        }
        if (TIMESTAMP_COLUMNS.contains(column)) {
            return Timestamp.class;
        }
        if (BOOLEAN_COLUMNS.contains(column)) {
            return Boolean.class;
        }
        return String.class;
    }

    private Object convertType(Object value, String column, String table) {
        if (value == null) {
            return null;
        }
        if (FLOAT_COLUMNS.contains(column)) {
            return Double.parseDouble(value.toString());
        }
        if (NUMBER_COLUMNS.contains(column)) {
            return Long.valueOf(value.toString());
        }
        if (UUID_COLUMNS.contains(column)) {
            UUID uuid = null;
            try {
                uuid = UUID.fromString(value.toString());
            } catch (Exception e) {
                // 引用表没有对应的纪录，不用插入。
                // log.error("Exception when table={}
                // and column={} and value={}", table, column, value, e);
            }
            return uuid;
        }
        if (TIMESTAMP_COLUMNS.contains(column)) {
            if (value instanceof LocalDateTime) {
                return DateTimeConverter.toTimestamp((LocalDateTime) value, ZoneId.systemDefault());
            }
            if (value instanceof String s && !s.isEmpty()) {
                LocalDateTime time = LocalDateTime.parse(s, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return DateTimeConverter.toTimestamp(time, ZoneId.systemDefault());
            }
            return null;
        }
        if (BOOLEAN_COLUMNS.contains(column)) {
            if (value instanceof Boolean) {
                return value;
            }
            if (value instanceof Number n) {
                return n.intValue() != 0;
            }
            if (value instanceof String s) {
                return Boolean.parseBoolean(s);
            }
        }
        return value;
    }
}
