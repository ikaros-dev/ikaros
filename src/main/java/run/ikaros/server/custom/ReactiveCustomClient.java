package run.ikaros.server.custom;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.function.Predicate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.infra.warp.PagingWrap;


public interface ReactiveCustomClient {

    <C> Mono<C> create(C custom);

    <C> Mono<C> update(C custom);

    <C> Mono<C> delete(C custom);

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
