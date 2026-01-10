package run.ikaros.server.store.repository;

import java.util.UUID;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import reactor.core.publisher.Mono;

@NoRepositoryBean
public interface BaseRepository<T>
    extends ReactiveCrudRepository<T, UUID>, ReactiveSortingRepository<T, UUID>,
    ReactiveQueryByExampleExecutor<T> {

    Mono<T> insert(T objToInsert);

    Mono<T> update(T objToUpdate);

}
