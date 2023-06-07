package run.ikaros.server.custom;

import static run.ikaros.server.custom.CustomConverter.getNameFieldValue;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Predicate;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Predicates;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.custom.Custom;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.wrap.PagingWrap;
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
        return Mono.justOrEmpty(custom)
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'custom' must not null")))
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
        return Mono.justOrEmpty(custom)
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'custom' must not null")))
            .flatMap(obj -> findCustomEntityOne(custom.getClass(), getNameFieldValue(custom)))
            .switchIfEmpty(Mono.error(
                new NotFoundException("custom not found for name=" + getNameFieldValue(custom))))
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
            .then(Mono.justOrEmpty(custom));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public <C> Mono<C> delete(C custom) {
        return Mono.justOrEmpty(custom)
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'custom' must not null")))
            .flatMap(obj -> findCustomEntityOne(custom.getClass(),
                getNameFieldValue(custom)))
            .flatMap(customEntity -> repository.delete(customEntity)
                .then(Mono.just(customEntity)))
            .flatMap(customEntity -> metadataRepository.deleteAllByCustomId(
                customEntity.getId()))
            .then(Mono.justOrEmpty(custom));
    }

    @Override
    public <C> Mono<C> delete(Class<C> clazz, String name) {
        return Mono.justOrEmpty(clazz)
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'clazz' must not null")))
            .flatMap(obj -> Mono.justOrEmpty(name))
            .filter(StringUtils::hasText)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'name' must has text")))
            .flatMap(n -> findOne(clazz, n))
            .flatMap(this::delete);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> deleteAll() {
        return metadataRepository.deleteAll()
            .then(repository.deleteAll());
    }

    private <C> Mono<CustomEntity> findCustomEntityOne(Class<C> type, String name) {
        return Mono.justOrEmpty(type)
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'type' must not null")))
            .flatMap(obj -> Mono.justOrEmpty(name))
            .filter(StringUtils::hasText)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'name' must has text")))
            .flatMap(obj -> repository.findOne(Example.of(CustomEntity.builder()
                .group(type.getAnnotation(Custom.class).group())
                .version(type.getAnnotation(Custom.class).version())
                .kind(type.getAnnotation(Custom.class).kind())
                .name(name)
                .build())));
    }

    @Override
    public <C> Mono<C> findOne(Class<C> type, String name) {
        return Mono.justOrEmpty(type)
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'type' must not null")))
            .flatMap(obj -> Mono.justOrEmpty(name))
            .filter(StringUtils::hasText)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'name' must has text")))
            .flatMap(obj -> findCustomEntityOne(type, name))
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
    public <C> Mono<PagingWrap<C>> findAllWithPage(Class<C> type, Integer page, Integer size,
                                                   Predicate<C> predicate) {
        final int finalPage = (page == null || page <= 0) ? 1 : page;
        final int finalSize = (size == null || size <= 0) ? 5 : size;
        return Mono.justOrEmpty(type)
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'type' must not null")))
            .flatMap(obj -> repository.findAllByGroupAndVersionAndKind(
                    type.getAnnotation(Custom.class).group(),
                    type.getAnnotation(Custom.class).version(),
                    type.getAnnotation(Custom.class).kind(),
                    PageRequest.of(finalPage - 1, finalSize))
                .flatMap(customEntity -> metadataRepository.findAllByCustomId(customEntity.getId())
                    .collectList()
                    .flatMap(customMetadataEntities ->
                        Mono.just(new CustomDto(customEntity, customMetadataEntities))))
                .map(customDto -> CustomConverter.convertFrom(type, customDto))
                .filter(Objects.isNull(predicate) ? Predicates.isTrue() : predicate)
                .collectList()
                .flatMap(customList ->
                    repository.countCustomEntitiesByGroupAndVersionAndKind(
                        type.getAnnotation(Custom.class).group(),
                        type.getAnnotation(Custom.class).version(),
                        type.getAnnotation(Custom.class).kind())
                        .map(count -> new PagingWrap<C>(finalPage, finalSize, count, customList))));
    }

    @Override
    public <C> Flux<C> findAll(Class<C> type, Predicate<C> predicate) {
        return Mono.justOrEmpty(type)
            .filter(Objects::nonNull)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("'type' must not null")))
            .flatMapMany(obj -> repository.findAll(Example.of(CustomEntity.builder()
                .group(type.getAnnotation(Custom.class).group())
                .version(type.getAnnotation(Custom.class).version())
                .kind(type.getAnnotation(Custom.class).kind())
                .build())))
            .flatMap(customEntity -> metadataRepository.findAllByCustomId(customEntity.getId())
                .collectList()
                .flatMap(customMetadataEntityList ->
                    Mono.just(new CustomDto(customEntity, customMetadataEntityList))))
            .map(customDto -> CustomConverter.convertFrom(type, customDto))
            .filter(Objects.isNull(predicate) ? Predicates.isTrue() : predicate);
    }

}
