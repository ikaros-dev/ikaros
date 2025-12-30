package run.ikaros.server.core.episode;

import java.util.List;
import java.util.UUID;
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

    Mono<Episode> findById(UUID episodeId);

    Flux<Episode> findAllBySubjectId(UUID subjectId);

    Flux<EpisodeRecord> findRecordsBySubjectId(UUID subjectId);

    Mono<Episode> findBySubjectIdAndGroupAndSequenceAndName(
        UUID subjectId, EpisodeGroup group, Float sequence, String name);

    Flux<Episode> findBySubjectIdAndGroupAndSequence(UUID subjectId, EpisodeGroup group,
                                                     Float sequence);

    Mono<Void> deleteById(UUID episodeId);

    Mono<Long> countBySubjectId(UUID subjectId);

    /**
     * 当前条目已经绑定附件的剧集数量.
     */
    Mono<Long> countMatchingBySubjectId(UUID subjectId);

    Flux<EpisodeResource> findResourcesById(UUID episodeId);

    /**
     * 更新条目的剧集，逻辑是删除旧的添加新的.
     */
    @Transactional
    Flux<Episode> updateEpisodesWithSubjectId(UUID subjectId, List<Episode> episodes);
}
