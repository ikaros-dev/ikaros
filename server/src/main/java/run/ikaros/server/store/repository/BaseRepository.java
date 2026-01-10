package run.ikaros.server.store.repository;

import java.util.UUID;
import org.reactivestreams.Publisher;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface BaseRepository<T>
    extends ReactiveCrudRepository<T, UUID>, ReactiveSortingRepository<T, UUID>,
    ReactiveQueryByExampleExecutor<T> {

    /**
     * 在手动生成UUID的情况下，直接save无法正常插入,
     * 请使用 {@link #insert(Object)} 或者 {@link  #update(Object)}
     * .
     *
     * @since 1.1
     */
    @Deprecated(since = "v1.1")
    @Override
    <S extends T> Mono<S> save(S entity);

    /**
     * 在手动生成UUID的情况下，直接save无法正常插入,
     * 请使用 {@link #insert(Object)} 或者 {@link  #update(Object)}
     * .
     *
     * @since 1.1
     */
    @Deprecated(since = "v1.1")
    @Override
    <S extends T> Flux<S> saveAll(Iterable<S> entities);

    /**
     * 在手动生成UUID的情况下，直接save无法正常插入,
     * 请使用 {@link #insert(Object)} 或者 {@link  #update(Object)}
     * .
     *
     * @since 1.1
     */
    @Deprecated(since = "v1.1")
    @Override
    <S extends T> Flux<S> saveAll(Publisher<S> entityStream);

    Mono<T> insert(T objToInsert);

    Mono<T> update(T objToUpdate);

}
