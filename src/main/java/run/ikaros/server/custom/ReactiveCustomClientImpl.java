package run.ikaros.server.custom;

import java.util.Comparator;
import java.util.function.Predicate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.result.PageResult;
import run.ikaros.server.store.repository.CustomMetadataRepository;
import run.ikaros.server.store.repository.CustomRepository;

@Component
public class ReactiveCustomClientImpl implements ReactiveCustomClient {

    private final CustomRepository customRepository;
    private final CustomMetadataRepository metadataRepository;

    public ReactiveCustomClientImpl(CustomRepository customRepository,
                                    CustomMetadataRepository metadataRepository) {
        this.customRepository = customRepository;
        this.metadataRepository = metadataRepository;
    }

    @Override
    public <E> E create(E extension) {
        return null;
    }

    @Override
    public <E> E update(E extension) {
        return null;
    }

    @Override
    public <E> E delete(E extension) {
        return null;
    }

    @Override
    public <E> Mono<E> get(Class<E> type, String name) {
        return null;
    }

    @Override
    public <E> Mono<E> fetch(Class<E> type, String name) {
        return null;
    }

    @Override
    public <E> Mono<PageResult<E>> list(Class<E> type, Predicate<E> predicate,
                                        Comparator<E> comparator, int page, int size) {
        return null;
    }

    @Override
    public <E> Flux<E> list(Class<E> type, Predicate<E> predicate, Comparator<E> comparator) {
        return null;
    }
}
