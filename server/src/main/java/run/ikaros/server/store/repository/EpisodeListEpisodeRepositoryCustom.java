package run.ikaros.server.store.repository;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Mono;

/** 歌单歌曲关系的 PostgreSQL 批量写入仓储片段. */
public interface EpisodeListEpisodeRepositoryCustom {

    /**
     * 批量插入指定歌单的歌曲关系.
     *
     * @param episodeListId 歌单 ID
     * @param episodeIds 歌曲 ID 列表
     * @return 插入完成信号
     */
    Mono<Void> insertAll(UUID episodeListId, List<UUID> episodeIds);
}
