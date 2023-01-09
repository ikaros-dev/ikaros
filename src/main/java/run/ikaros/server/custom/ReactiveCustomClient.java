package run.ikaros.server.custom;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Comparator;
import java.util.function.Predicate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.result.PageResult;


public interface ReactiveCustomClient {

    <C> Mono<C> create(C custom);

    <C> Mono<C> update(C custom);

    <C> Mono<C> delete(C custom);

    Mono<Void> deleteAll();

    <C> Mono<C> findOne(Class<C> type, String name);

    <C> Mono<PageResult<C>> findAllWithPage(@Nonnull Class<C> type,
                                            @Nullable int page, @Nullable int size,
                                            @Nullable Predicate<C> predicate,
                                            @Nullable Comparator<C> comparator);

    <C> Flux<C> findAll(@Nonnull Class<C> type, @Nullable Predicate<C> predicate);

}
