package run.ikaros.server.migration;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.utils.UuidV7Utils;

@Slf4j
@Component
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

    private final R2dbcEntityTemplate oldEntityTemplate;
    private final R2dbcEntityTemplate newEntityTemplate;

    public MigrationInitializer(R2dbcEntityTemplate oldEntityTemplate,
                                @Qualifier("migrationR2dbcEntityTemplate") R2dbcEntityTemplate newEntityTemplate) {
        this.oldEntityTemplate = oldEntityTemplate;
        this.newEntityTemplate = newEntityTemplate;
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
            .doOnSuccess(v -> log.info("Database migration UUID generation completed."));
    }
}
