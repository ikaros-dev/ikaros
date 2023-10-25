package run.ikaros.api.custom;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import java.util.function.Predicate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.model.PagingWrap;


public interface ReactiveCustomClient {

    <C> Mono<C> create(C custom);

    <C> Mono<C> update(C custom);

    <C> Mono<Void> updateOneMeta(@Nonnull Class<C> clazz, @NotBlank String name,
                                 @NotBlank String metaName, @Nullable byte[] metaNewVal);

    <C> Mono<byte[]> fetchOneMeta(@Nonnull Class<C> clazz, @NotBlank String name,
                                  @NotBlank String metaName);

    <C> Mono<C> delete(C custom);

    <C> Mono<C> delete(Class<C> clazz, String name);

    Mono<Void> deleteAll();

    <C> Mono<C> findOne(Class<C> type, String name);

    /**
     * find all with page.
     *
     * @param type      custom class type
     * @param page      start for 1
     * @param size      size
     * @param predicate predicate
     * @param <C>       custom class type
     * @return PagingWrap
     */
    <C> Mono<PagingWrap<C>> findAllWithPage(@Nonnull Class<C> type,
                                            @Nullable Integer page, @Nullable Integer size,
                                            @Nullable Predicate<C> predicate);

    <C> Flux<C> findAll(@Nonnull Class<C> type, @Nullable Predicate<C> predicate);

}
