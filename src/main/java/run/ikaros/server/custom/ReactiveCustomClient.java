package run.ikaros.server.custom;

import java.util.Comparator;
import java.util.function.Predicate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.result.PageResult;


public interface ReactiveCustomClient {

    <E> E create(E extension);

    <E> E update(E extension);

    <E> E delete(E extension);

    <E> Mono<E> get(Class<E> type, String name);

    <E> Mono<E> fetch(Class<E> type, String name);

    <E> Mono<PageResult<E>> list(Class<E> type, Predicate<E> predicate,
                                                   Comparator<E> comparator, int page, int size);

    <E> Flux<E> list(Class<E> type, Predicate<E> predicate,
                                       Comparator<E> comparator);
}
