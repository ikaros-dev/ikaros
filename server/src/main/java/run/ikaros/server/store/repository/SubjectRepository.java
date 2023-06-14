package run.ikaros.server.store.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.SubjectEntity;

public interface SubjectRepository extends R2dbcRepository<SubjectEntity, Long> {
    Mono<SubjectEntity> findByBgmtvId(Long bgmtvId);

    Mono<Boolean> existsById(Long id);

    Flux<SubjectEntity> findAllBy(Pageable pageable);

    Flux<SubjectEntity> findAllByNsfw(Boolean nsfw, Pageable pageable);

    Mono<Long> countAllByNsfw(Boolean nsfw);

    Flux<SubjectEntity> findAllByType(Integer type, Pageable pageable);

    Mono<Long> countAllByType(Integer type);

    Flux<SubjectEntity> findAllByNsfwAndType(Boolean nsfw, Integer type, Pageable pageable);

    Mono<Long> countAllByNsfwAndType(Boolean nsfw, Integer type);

    Flux<SubjectEntity> findAllByNameCnLike(String nameCn, Pageable pageable);

    Mono<Long> countAllByNameCnLike(String nameCn);

    Flux<SubjectEntity> findAllByNsfwAndNameCnLike(Boolean nsfw, String nameCn, Pageable pageable);

    Mono<Long> countAllByNsfwAndNameCnLike(Boolean nsfw, String nameCn);

    Flux<SubjectEntity> findAllByNameCnLikeAndType(String nameCn, Integer type,
                                                   Pageable pageable);

    Mono<Long> countAllByNameCnLikeAndType(String nameCn, Integer type);


    Flux<SubjectEntity> findAllByNsfwAndNameCnLikeAndType(Boolean nsfw, String nameCn, Integer type,
                                                          Pageable pageable);

    Mono<Long> countAllByNsfwAndNameCnLikeAndType(Boolean nsfw, String nameCn, Integer type);

    Flux<SubjectEntity> findAllByNameLike(String name, Pageable pageable);

    Mono<Long> countAllByNameLike(String name);

    Flux<SubjectEntity> findAllByNsfwAndNameLike(Boolean nsfw, String name, Pageable pageable);

    Mono<Long> countAllByNsfwAndNameLike(Boolean nsfw, String name);

    Flux<SubjectEntity> findAllByNameLikeAndType(String name, Integer type,
                                                 Pageable pageable);

    Mono<Long> countAllByNameLikeAndType(String name, Integer type);


    Flux<SubjectEntity> findAllByNsfwAndNameLikeAndType(Boolean nsfw, String name, Integer type,
                                                        Pageable pageable);

    Mono<Long> countAllByNsfwAndNameLikeAndType(Boolean nsfw, String name, Integer type);

    Flux<SubjectEntity> findAllByNameLikeAndNameCnLike(String name,
                                                       String nameCn, Pageable pageable);

    Mono<Long> countAllByNameLikeAndNameCnLike(String name,
                                               String nameCn);

    Flux<SubjectEntity> findAllByNsfwAndNameLikeAndNameCnLike(Boolean nsfw, String name,
                                                              String nameCn, Pageable pageable);

    Mono<Long> countAllByNsfwAndNameLikeAndNameCnLike(Boolean nsfw, String name,
                                                      String nameCn);

    Flux<SubjectEntity> findAllByNameLikeAndNameCnLikeAndType(String name,
                                                              String nameCn, Integer type,
                                                              Pageable pageable);

    Mono<Long> countAllByNameLikeAndNameCnLikeAndType(String name,
                                                      String nameCn, Integer type);

    Flux<SubjectEntity> findAllByNsfwAndNameLikeAndNameCnLikeAndType(Boolean nsfw, String name,
                                                                     String nameCn, Integer type,
                                                                     Pageable pageable);

    Mono<Long> countAllByNsfwAndNameLikeAndNameCnLikeAndType(Boolean nsfw, String name,
                                                             String nameCn, Integer type);
}
