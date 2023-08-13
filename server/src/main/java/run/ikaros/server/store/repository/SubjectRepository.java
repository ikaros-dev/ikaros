package run.ikaros.server.store.repository;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.store.entity.SubjectEntity;

public interface SubjectRepository extends R2dbcRepository<SubjectEntity, Long> {

    @NotNull Mono<Boolean> existsById(@NotNull Long id);

    Flux<SubjectEntity> findAllBy(Pageable pageable);

    Flux<SubjectEntity> findAllByOrderByAirTimeDesc(Pageable pageable);

    Flux<SubjectEntity> findAllByNsfw(Boolean nsfw, Pageable pageable);

    Mono<Long> countAllByNsfw(Boolean nsfw);

    Flux<SubjectEntity> findAllByType(SubjectType type, Pageable pageable);

    Mono<Long> countAllByType(SubjectType type);

    Flux<SubjectEntity> findAllByNsfwAndType(Boolean nsfw, SubjectType type, Pageable pageable);

    Mono<Long> countAllByNsfwAndType(Boolean nsfw, SubjectType type);

    Flux<SubjectEntity> findAllByNameCnLike(String nameCn, Pageable pageable);

    Mono<Long> countAllByNameCnLike(String nameCn);

    Flux<SubjectEntity> findAllByNsfwAndNameCnLike(Boolean nsfw, String nameCn, Pageable pageable);

    Mono<Long> countAllByNsfwAndNameCnLike(Boolean nsfw, String nameCn);

    Flux<SubjectEntity> findAllByNameCnLikeAndType(String nameCn, SubjectType type,
                                                   Pageable pageable);

    Mono<Long> countAllByNameCnLikeAndType(String nameCn, SubjectType type);


    Flux<SubjectEntity> findAllByNsfwAndNameCnLikeAndType(Boolean nsfw, String nameCn,
                                                          SubjectType type,
                                                          Pageable pageable);

    Mono<Long> countAllByNsfwAndNameCnLikeAndType(Boolean nsfw, String nameCn, SubjectType type);

    Flux<SubjectEntity> findAllByNameLike(String name, Pageable pageable);

    Mono<Long> countAllByNameLike(String name);

    Flux<SubjectEntity> findAllByNsfwAndNameLike(Boolean nsfw, String name, Pageable pageable);

    Mono<Long> countAllByNsfwAndNameLike(Boolean nsfw, String name);

    Flux<SubjectEntity> findAllByNameLikeAndType(String name, SubjectType type,
                                                 Pageable pageable);

    Mono<Long> countAllByNameLikeAndType(String name, SubjectType type);


    Flux<SubjectEntity> findAllByNsfwAndNameLikeAndType(Boolean nsfw, String name, SubjectType type,
                                                        Pageable pageable);

    Mono<Long> countAllByNsfwAndNameLikeAndType(Boolean nsfw, String name, SubjectType type);

    Flux<SubjectEntity> findAllByNameLikeAndNameCnLike(String name,
                                                       String nameCn, Pageable pageable);

    Mono<Long> countAllByNameLikeAndNameCnLike(String name,
                                               String nameCn);

    Flux<SubjectEntity> findAllByNsfwAndNameLikeAndNameCnLike(Boolean nsfw, String name,
                                                              String nameCn, Pageable pageable);

    Mono<Long> countAllByNsfwAndNameLikeAndNameCnLike(Boolean nsfw, String name,
                                                      String nameCn);

    Flux<SubjectEntity> findAllByNameLikeAndNameCnLikeAndType(String name,
                                                              String nameCn, SubjectType type,
                                                              Pageable pageable);

    Mono<Long> countAllByNameLikeAndNameCnLikeAndType(String name,
                                                      String nameCn, SubjectType type);

    Flux<SubjectEntity> findAllByNsfwAndNameLikeAndNameCnLikeAndType(Boolean nsfw, String name,
                                                                     String nameCn,
                                                                     SubjectType type,
                                                                     Pageable pageable);

    Mono<Long> countAllByNsfwAndNameLikeAndNameCnLikeAndType(Boolean nsfw, String name,
                                                             String nameCn, SubjectType type);
}
