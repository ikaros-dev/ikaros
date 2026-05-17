package run.ikaros.server.migration;

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

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Component
@ConditionalOnProperty(name = "ikaros.migration.enable", havingValue = "true", matchIfMissing = false)
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
        putFk("attachment", "parent_id", "attachment");
        putFk("attachment", "driver_id", "attachment_driver");
        putFk("attachment_relation", "attachment_id", "attachment");
        putFk("attachment_relation", "relation_attachment_id", "attachment");
        putFk("attachment_reference", "attachment_id", "attachment");
        putFk("attachment_reference", "reference_id", "subject");
        putFk("episode_collection", "user_id", "ikuser");
        putFk("episode_collection", "subject_id", "subject");
        putFk("episode_collection", "episode_id", "episode");
        putFk("episode_list_episode", "episode_list_id", "episode_list");
        putFk("episode_list_episode", "episode_id", "episode");
        putFk("episode_list_collection", "user_id", "ikuser");
        putFk("episode_list_collection", "episode_list_id", "episode_list");
        putFk("person_character", "person_id", "person");
        putFk("person_character", "character_id", "character");
        putFk("role_authority", "role_id", "role");
        putFk("role_authority", "authority_id", "authority");
        putFk("subject_character", "subject_id", "subject");
        putFk("subject_character", "character_id", "character");
        putFk("subject_collection", "user_id", "ikuser");
        putFk("subject_collection", "subject_id", "subject");
        putFk("subject_person", "subject_id", "subject");
        putFk("subject_person", "person_id", "person");
        putFk("subject_relation", "subject_id", "subject");
        putFk("subject_relation", "relation_subject_id", "subject");
        putFk("subject_sync", "subject_id", "subject");
        putFk("tag", "user_id", "ikuser");
        putFk("tag", "master_id", "subject");
        putFk("custom_metadata", "custom_id", "custom");
        putFk("attachment_driver", "user_id", "ikuser");
    }

    private static void putFk(String table, String column, String refTable) {
        FK_COLUMNS.computeIfAbsent(table, k -> new HashMap<>()).put(column, refTable);
    }

    private static final String MIGRATION_SCRIPT_PATTERN = "classpath:db/migration/*.sql";

    private static final Set<String> TIMESTAMP_COLUMNS = Set.of(
            "create_time", "update_time", "expire_time", "air_time"
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
                                String uuid = UuidV7Utils.generate();
                                return client.sql("UPDATE " + table
                                                + " SET migration_uuid = :uuid WHERE id = :id and migration_uuid isnull")
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
        return Mono.fromCallable(() -> resourcePatternResolver.getResources(MIGRATION_SCRIPT_PATTERN))
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
            Object value = convertType(row.get(column), column);
            if (value == null) {
                spec = spec.bindNull("p" + i, String.class);
            } else {
                spec = spec.bind("p" + i, value);
            }
        }
        return spec.fetch().rowsUpdated().then();
    }

    private Object convertType(Object value, String column) {
        if (value == null) {
            return null;
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
