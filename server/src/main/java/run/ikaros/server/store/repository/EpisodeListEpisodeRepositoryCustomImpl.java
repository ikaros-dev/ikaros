package run.ikaros.server.store.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** 使用 PostgreSQL 数组展开批量写入歌单歌曲关系. */
public class EpisodeListEpisodeRepositoryCustomImpl
    implements EpisodeListEpisodeRepositoryCustom {

    /** 单批写入的最大关系数量. */
    private static final int BATCH_SIZE = 1000;

    /** PostgreSQL 歌单歌曲关系批量插入语句. */
    private static final String INSERT_ALL_SQL = """
        INSERT INTO episode_list_episode (episode_list_id, episode_id)
        SELECT :episodeListId, episode_id
        FROM unnest(CAST(:episodeIds AS uuid[])) AS episode_ids(episode_id)
        """;

    /** 执行响应式 SQL 的数据库客户端. */
    private final DatabaseClient databaseClient;

    /**
     * 创建歌单歌曲关系仓储片段.
     *
     * @param databaseClient 数据库客户端
     */
    public EpisodeListEpisodeRepositoryCustomImpl(DatabaseClient databaseClient) {
        this.databaseClient = databaseClient;
    }

    @Override
    public Mono<Void> insertAll(UUID episodeListId, List<UUID> episodeIds) {
        Assert.notNull(episodeListId, "'episodeListId' must not be null");
        Assert.notNull(episodeIds, "'episodeIds' must not be null");
        if (episodeIds.isEmpty()) {
            return Mono.empty();
        }
        return Flux.fromIterable(episodeIds)
            .buffer(BATCH_SIZE)
            .concatMap(batch -> databaseClient.sql(INSERT_ALL_SQL)
                .bind("episodeListId", episodeListId)
                .bind("episodeIds", batch.toArray(UUID[]::new))
                .fetch()
                .rowsUpdated())
            .then();
    }
}
