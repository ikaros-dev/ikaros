package run.ikaros.server.custom;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.data.domain.Example;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.server.core.result.PageResult;
import run.ikaros.server.infra.exception.NotFoundException;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;
import run.ikaros.server.store.repository.CustomMetadataRepository;
import run.ikaros.server.store.repository.CustomRepository;

@Service
public class ReactiveCustomClientImpl implements ReactiveCustomClient {

    private final CustomRepository repository;
    private final CustomMetadataRepository metadataRepository;

    public ReactiveCustomClientImpl(CustomRepository repository,
                                    CustomMetadataRepository metadataRepository) {
        this.repository = repository;
        this.metadataRepository = metadataRepository;
    }


    @Override
    @SuppressWarnings("unchecked")
    @Transactional(rollbackFor = Exception.class)
    public <C> Mono<C> create(C custom) {
        Assert.notNull(custom, "'custom' must not null");
        return Mono.just(custom)
            .map(CustomConverter::convertTo)
            .flatMap(customDto -> repository.save(customDto.customEntity())
                .flatMap(customEntity -> Mono.just(customDto)))
            .flatMap(customDto -> Mono.just(customDto.customMetadataEntityList())
                .filter(customMetadataEntityList -> !customMetadataEntityList.isEmpty())
                .flatMapMany(customMetadataEntityList -> Flux.fromStream(
                    customMetadataEntityList.stream()))
                .map(customMetadataEntity ->
                    customMetadataEntity.setCustomId(customDto.customEntity().getId()))
                .flatMap(metadataRepository::save)
                .collectList()
                .flatMap(customMetadataEntityList -> Mono.just(customDto)))
            .map(customDto -> (C) CustomConverter.convertFrom(custom.getClass(), customDto));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <C> Mono<C> update(C custom) {
        Assert.notNull(custom, "'custom' must not null");
        String name = CustomConverter.getNameFieldValue(custom);
        return findCustomEntityOne(custom.getClass(), name)
            .switchIfEmpty(Mono.error(new NotFoundException("custom not found for name=" + name)))
            .flatMap(customEntity -> Mono.just(custom)
                .map(CustomConverter::convertTo)
                .flatMap(customDto -> Mono.just(customDto)
                    .flatMap(customDtoTmp -> Mono.just(customDtoTmp.customEntity()))
                    .map(customEntityTmp -> customEntityTmp.setId(customEntity.getId()))
                    .then(Mono.just(customDto)))
                .map(CustomDto::updateMetadataCustomId)
                .flatMap(customDto -> Mono.just(customDto.customMetadataEntityList())
                    .filter(customMetadataEntityList -> !customMetadataEntityList.isEmpty())
                    .flatMapMany(customMetadataEntityList -> Flux.fromStream(
                        customMetadataEntityList.stream()))
                    .flatMap(customMetadataEntity -> metadataRepository.findByCustomIdAndKey(
                            customMetadataEntity.getCustomId(), customMetadataEntity.getKey())
                        .switchIfEmpty(metadataRepository.save(customMetadataEntity))
                        .filter(existsCustomMetadataEntity ->
                            !(Objects.nonNull(existsCustomMetadataEntity.getValue())
                                && Objects.nonNull(customMetadataEntity.getValue())
                                && Arrays.equals(existsCustomMetadataEntity.getValue(),
                                customMetadataEntity.getValue()))
                        )
                        .flatMap(existsCustomMetadataEntity -> metadataRepository.save(
                            existsCustomMetadataEntity.setValue(customMetadataEntity.getValue()))
                        )
                    )
                    .then()
                )
            )
            .then(Mono.just(custom));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <C> Mono<C> delete(C custom) {
        Assert.notNull(custom, "'custom' must not null");
        return findCustomEntityOne(custom.getClass(), CustomConverter.getNameFieldValue(custom))
            .flatMap(customEntity -> repository.delete(customEntity)
                .then(Mono.just(customEntity)))
            .flatMap(customEntity -> metadataRepository.deleteAllByCustomId(
                customEntity.getId()))
            .then(Mono.just(custom));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> deleteAll() {
        return metadataRepository.deleteAll()
            .then(repository.deleteAll());
    }

    private <C> Mono<CustomEntity> findCustomEntityOne(Class<C> type, String name) {
        Assert.notNull(type, "'type' must not null");
        Assert.hasText(name, "'name' must has text");
        Custom annotation = type.getAnnotation(Custom.class);
        return repository.findOne(Example.of(CustomEntity.builder()
            .group(annotation.group())
            .version(annotation.version())
            .kind(annotation.kind())
            .name(name)
            .build()));
    }

    @Override
    public <C> Mono<C> findOne(Class<C> type, String name) {
        Assert.notNull(type, "'type' must not null");
        Assert.hasText(name, "'name' must has text");
        return findCustomEntityOne(type, name)
            .switchIfEmpty(Mono.error(new NotFoundException("custom not found for name=" + name)))
            .flatMap(customEntity -> metadataRepository.findAll(
                    Example.of(CustomMetadataEntity.builder()
                        .customId(customEntity.getId())
                        .build()))
                .collectList()
                .flatMap(customMetadataEntityList -> Mono.just(
                    new CustomDto(customEntity, customMetadataEntityList)))
            )
            .map(customDto -> CustomConverter.convertFrom(type, customDto));
    }

    @Override
    public <C> Mono<PageResult<C>> findAllWithPage(Class<C> type, Predicate<C> predicate,
                                                   Comparator<C> comparator, int page, int size) {
        return null;
    }

    @Override
    public <C> Flux<C> findAll(Class<C> type, Predicate<C> predicate, Comparator<C> comparator) {
        return null;
    }
}
