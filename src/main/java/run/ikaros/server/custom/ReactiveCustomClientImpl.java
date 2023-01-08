package run.ikaros.server.custom;

import java.util.Comparator;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.result.PageResult;
import run.ikaros.server.store.repository.CustomMetadataRepository;
import run.ikaros.server.store.repository.CustomRepository;

@Service
public class ReactiveCustomClientImpl implements ReactiveCustomClient {

    private final CustomRepository customRepository;
    private final CustomMetadataRepository metadataRepository;

    public ReactiveCustomClientImpl(CustomRepository customRepository,
                                    CustomMetadataRepository metadataRepository) {
        this.customRepository = customRepository;
        this.metadataRepository = metadataRepository;
    }


    @Override
    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public <C> Mono<C> create(C custom) {
        return Mono.just(custom)
            .map(CustomConverter::convertTo)
            .flatMap(customDto -> customRepository.save(customDto.customEntity())
                .flatMap(customEntity -> Mono.just(customDto)))
            .map(customDto -> {
                Mono.just(customDto.customMetadataEntityList())
                    .filter(customMetadataEntityList -> !customMetadataEntityList.isEmpty())
                    .flatMapMany(customMetadataEntityList -> Flux.fromStream(
                        customMetadataEntityList.stream()))
                    .map(metadataRepository::save)

                ;
                return customDto;
            })
            .map(customDto -> (C) CustomConverter.convertFrom(custom.getClass(), customDto));
    }

    @Override
    public <C> Mono<C> update(C custom) {
        return null;
    }

    @Override
    public <C> Mono<C> delete(C custom) {
        return null;
    }

    @Override
    public <C> Mono<C> get(Class<C> type, String name) {
        return null;
    }

    @Override
    public <C> Mono<C> fetch(Class<C> type, String name) {
        return null;
    }

    @Override
    public <C> Mono<PageResult<C>> list(Class<C> type, Predicate<C> predicate,
                                        Comparator<C> comparator, int page, int size) {
        return null;
    }

    @Override
    public <C> Flux<C> list(Class<C> type, Predicate<C> predicate, Comparator<C> comparator) {
        return null;
    }
}
