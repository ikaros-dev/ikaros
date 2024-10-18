package run.ikaros.api.core.subject;

import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.plugin.AllowPluginOperate;
import run.ikaros.api.store.enums.EpisodeGroup;

public interface EpisodeOperate extends AllowPluginOperate {
    Mono<Episode> save(Episode episode);

    Mono<Episode> findById(Long episodeId);

    Flux<Episode> findAllBySubjectId(Long subjectId);

    Mono<Episode> findBySubjectIdAndGroupAndSequenceAndName(
        Long subjectId, EpisodeGroup group, Float sequence, String name);

    Flux<Episode> findBySubjectIdAndGroupAndSequence(Long subjectId, EpisodeGroup group,
                                                     Float sequence);

    Mono<Long> countBySubjectId(Long subjectId);

    /**
     * 当前条目已经绑定附件的剧集数量.
     */
    Mono<Long> countMatchingBySubjectId(Long subjectId);

    Flux<EpisodeResource> findResourcesById(Long episodeId);

    /**
     * 更新条目的剧集，逻辑是删除旧的添加新的.
     */
    Flux<Episode> updateEpisodesWithSubjectId(Long subjectId, List<Episode> episodes);
}
