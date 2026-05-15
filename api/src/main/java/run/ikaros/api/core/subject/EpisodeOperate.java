package run.ikaros.api.core.subject;

import java.util.List;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.EpisodeGroup;

public interface EpisodeOperate extends AllowPluginOperate {
    Mono<Episode> save(Episode episode);

    Mono<Episode> findById(UUID episodeId);

    Flux<Episode> findAllBySubjectId(UUID subjectId);

    Mono<Episode> findBySubjectIdAndGroupAndSequenceAndName(
        UUID subjectId, EpisodeGroup group, Float sequence, String name);

    Flux<Episode> findBySubjectIdAndGroupAndSequence(UUID subjectId, EpisodeGroup group,
                                                     Float sequence);

    Mono<Long> countBySubjectId(UUID subjectId);

    /**
     * 当前条目已经绑定附件的剧集数量.
     */
    Mono<Long> countMatchingBySubjectId(UUID subjectId);

    Flux<EpisodeResource> findResourcesById(UUID episodeId);

    /**
     * 更新条目的剧集，逻辑是删除旧的添加新的.
     */
    Flux<Episode> updateEpisodesWithSubjectId(UUID subjectId, List<Episode> episodes);
}
