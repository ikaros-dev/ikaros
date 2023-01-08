package run.ikaros.server.custom;

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

    <C> Mono<C> get(Class<C> type, String name);

    <C> Mono<C> fetch(Class<C> type, String name);

    <C> Mono<PageResult<C>> list(Class<C> type, Predicate<C> predicate,
                                                   Comparator<C> comparator, int page, int size);

    <C> Flux<C> list(Class<C> type, Predicate<C> predicate,
                                       Comparator<C> comparator);
}
