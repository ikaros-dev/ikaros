package run.ikaros.server.core.episode;

import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeRecord;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.store.enums.EpisodeGroup;

public interface EpisodeService {
    @Transactional
    Mono<Episode> save(Episode episode);

    Mono<Episode> findById(@Nullable UUID episodeId);

    Flux<Episode> findAllBySubjectId(@Nullable UUID subjectId);

    Flux<EpisodeRecord> findRecordsBySubjectId(@Nullable UUID subjectId);

    Mono<Episode> findBySubjectIdAndGroupAndSequenceAndName(
        @Nullable UUID subjectId, EpisodeGroup group, Float sequence, String name);

    Flux<Episode> findBySubjectIdAndGroupAndSequence(UUID subjectId, EpisodeGroup group,
                                                     Float sequence);

    Mono<Void> deleteById(@Nullable UUID episodeId);

    Mono<Long> countBySubjectId(@Nullable UUID subjectId);

    /**
     * 当前条目已经绑定附件的剧集数量.
     */
    Mono<Long> countMatchingBySubjectId(@Nullable UUID subjectId);

    /**
     * 查询剧集的媒体资源，并投影本地扫描产生的内嵌和外置轨道信息.
     *
     * @param episodeId 剧集标识
     * @return 按自然数字顺序排列的剧集媒体资源
     */
    Flux<EpisodeResource> findResourcesById(@Nullable UUID episodeId);

    /**
     * 更新条目的剧集，逻辑是删除旧的添加新的.
     */
    @Transactional
    Flux<Episode> updateEpisodesWithSubjectId(UUID subjectId, List<Episode> episodes);
}
