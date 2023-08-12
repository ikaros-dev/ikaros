package run.ikaros.server.store.repository;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.VideoSubtitleEntity;

public interface VideoSubtitleRepository extends R2dbcRepository<VideoSubtitleEntity, Long> {
    Mono<Boolean> existsByVideoFileIdAndSubtitleFileId(Long videoFileId, Long subtitleFileId);
 
    Mono<VideoSubtitleEntity> findByVideoFileIdAndSubtitleFileId(Long videoFileId,
                                                                 Long subtitleFileId);

    Mono<Void> removeByVideoFileIdAndSubtitleFileId(Long videoFileId, Long subtitleFileId);

    Flux<VideoSubtitleEntity> findAllByVideoFileId(Long videoFileId);
}
