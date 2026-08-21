package run.ikaros.server.custom;

import static run.ikaros.api.constant.AppConst.BLOCK_TIMEOUT;
import static run.ikaros.server.custom.CustomConverter.getNameFieldValue;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.util.Predicates;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.custom.Custom;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;
import run.ikaros.server.store.repository.CustomMetadataRepository;
import run.ikaros.server.store.repository.CustomRepository;

@Deprecated
public class CustomClientImpl implements CustomClient {
    private final CustomRepository repository;
    private final CustomMetadataRepository metadataRepository;
    private final ReactiveTransactionManager reactiveTransactionManager;

    /**
     * Construct an instance.
     */
    public CustomClientImpl(CustomRepository repository,
                            CustomMetadataRepository metadataRepository,
                            R2dbcTransactionManager reactiveTransactionManager) {
        this.repository = repository;
        this.metadataRepository = metadataRepository;
        this.reactiveTransactionManager = reactiveTransactionManager;
    }


    @Override
    public <C> C create(C custom) {
        Assert.notNull(custom, "'custom' must not null.");
        CustomDto customDto = CustomConverter.convertTo(custom);
        CustomEntity customEntity = repository.save(customDto.customEntity()).block(BLOCK_TIMEOUT);
        if (customEntity == null) {
            throw new RuntimeException("Save custom entity fail for custom: " + custom);
        }
        List<CustomMetadataEntity> customMetadataEntities = customDto.customMetadataEntityList();
        if (customMetadataEntities != null
            && !customMetadataEntities.isEmpty()) {
            List<CustomMetadataEntity> newCustomMetadataEntities = new ArrayList<>();
            for (CustomMetadataEntity customMetadataEntity : customMetadataEntities) {
                customMetadataEntity.setCustomId(customEntity.getId());
                newCustomMetadataEntities.add(
                    metadataRepository.save(customMetadataEntity).block(BLOCK_TIMEOUT));
            }
            customMetadataEntities = newCustomMetadataEntities;
        }
        customDto = new CustomDto(customEntity, customMetadataEntities);
        return (C) CustomConverter.convertFrom(custom.getClass(), customDto);
    }

    private <C> CustomEntity findCustomEntityOne(Class<C> type, String name) {
        Assert.notNull(type, "'type' must not null.");
        Assert.hasText(name, "'name' must has text");
        Custom annotation = type.getAnnotation(Custom.class);
        return Mono.fromCallable(() -> repository.findOne(Example.of(CustomEntity.builder()
                .group(annotation.group())
                .version(annotation.version())
                .kind(annotation.kind())
                .name(name)
                .build())).block(BLOCK_TIMEOUT))
            .subscribeOn(Schedulers.boundedElastic()).block(BLOCK_TIMEOUT);
    }

    @Override
    public <C> C update(C custom) {
        Assert.notNull(custom, "'custom' must not null.");
        CustomEntity customEntity =
            findCustomEntityOne(custom.getClass(), getNameFieldValue(custom));
        if (customEntity == null) {
            throw new NotFoundException("custom not found for name=" + getNameFieldValue(custom));
        }
        UUID customEntityId = customEntity.getId();
        CustomDto customDto = CustomConverter.convertTo(custom);
        customEntity = customDto.customEntity();
        customEntity.setId(customEntityId);
        customDto.updateMetadataCustomId();

        List<CustomMetadataEntity> customMetadataEntities = customDto.customMetadataEntityList();
        if (customMetadataEntities != null
            && !customMetadataEntities.isEmpty()) {
            List<CustomMetadataEntity> newCustomMetadataEntities = new ArrayList<>();
            for (CustomMetadataEntity customMetadataEntity : customMetadataEntities) {
                CustomMetadataEntity existsCustomMetadataEntity =
                    metadataRepository.findByCustomIdAndKey(customMetadataEntity.getCustomId(),
                        customMetadataEntity.getKey()).block(BLOCK_TIMEOUT);
                if (existsCustomMetadataEntity == null) {
                    customMetadataEntity = metadataRepository.save(customMetadataEntity)
                        .block(BLOCK_TIMEOUT);
                } else {
                    if (customMetadataEntity.getValue() != null
                        && !customMetadataEntity.getValue()
                        .equals(existsCustomMetadataEntity.getValue())) {
                        customMetadataEntity =
                            metadataRepository.save(customMetadataEntity).block(BLOCK_TIMEOUT);
                    }
                }
                newCustomMetadataEntities.add(customMetadataEntity);
            }
            customMetadataEntities = newCustomMetadataEntities;
        }
        customDto = new CustomDto(customEntity, customMetadataEntities);
        return (C) CustomConverter.convertFrom(custom.getClass(), customDto);
    }

    @Override
    public <C> void updateOneMeta(@NotNull Class<C> clazz, String name, String metaName,
                                  @Nullable byte[] metaNewVal) {
        Assert.notNull(clazz, "'clazz' must not null.");
        Assert.isTrue(StringUtils.hasText(name), "'name' must has text");
        Assert.isTrue(StringUtils.hasText(metaName), "'metaName' must has text");
        Custom annotation = clazz.getAnnotation(Custom.class);
        byte[] bytes = fetchOneMeta(clazz, name, metaName);
        if (bytes == metaNewVal) {
            return;
        }

        TransactionalOperator rxtx =
            TransactionalOperator.create(reactiveTransactionManager);

        repository.findByGroupAndVersionAndKindAndName(annotation.group(),
                annotation.version(), annotation.kind(), name)
            .map(CustomEntity::getId)
            .flatMap(customId -> metadataRepository.updateValueByCustomIdAndKeyAndValue(
                customId, metaName, metaNewVal
            ))
            .as(rxtx::transactional).block(BLOCK_TIMEOUT);
    }

    @Override
    public <C> byte[] fetchOneMeta(@NotNull Class<C> clazz, String name, String metaName) {
        Assert.notNull(clazz, "'clazz' must not null.");
        Assert.isTrue(StringUtils.hasText(name), "'name' must has text");
        Assert.isTrue(StringUtils.hasText(metaName), "'metaName' must has text");
        CustomEntity customEntity = findCustomEntityOne(clazz, name);
        UUID customEntityId = customEntity.getId();

        CustomMetadataEntity customMetadata =
            metadataRepository.findByCustomIdAndKey(customEntityId, metaName)
                .block(BLOCK_TIMEOUT);
        if (customMetadata == null) {
            throw new NotFoundException("Not found metadata for class: " + clazz
                + ", name: " + name + ", metaName: " + metaName);
        }
        return customMetadata.getValue();
    }

    @Override
    public <C> void delete(C custom) {
        Assert.notNull(custom, "'custom' must not null.");
        CustomEntity customEntity =
            findCustomEntityOne(custom.getClass(), getNameFieldValue(custom));
        UUID customEntityId = customEntity.getId();
        repository.delete(customEntity).block(BLOCK_TIMEOUT);
        metadataRepository.deleteAllByCustomId(customEntityId)
            .block(BLOCK_TIMEOUT);
    }

    @Override
    public <C> C delete(Class<C> clazz, String name) {
        Assert.notNull(clazz, "'clazz' must not null.");
        Assert.hasText(name, "'name' must has text.");
        C custom = findOne(clazz, name);
        delete(custom);
        return null;
    }

    @Override
    public void deleteAll() {
        metadataRepository.deleteAll().block(BLOCK_TIMEOUT);
        repository.deleteAll().block(BLOCK_TIMEOUT);
    }

    @Override
    public <C> C findOne(Class<C> type, String name) {
        Assert.notNull(type, "'type' must not null.");
        Assert.hasText(name, "'name' must has text.");
        CustomEntity customEntity = findCustomEntityOne(type, name);
        if (customEntity == null) {
            throw new NotFoundException("custom not found for name=" + name);
        }
        UUID customEntityId = customEntity.getId();
        List<CustomMetadataEntity> customMetadataEntities =
            metadataRepository.findAllByCustomId(customEntityId).toStream()
                .toList();
        CustomDto customDto = new CustomDto(customEntity, customMetadataEntities);
        return CustomConverter.convertFrom(type, customDto);
    }

    @Override
    public <C> PagingWrap<C> findAllWithPage(@NotNull Class<C> type, @Nullable Integer page,
                                             @Nullable Integer size,
                                             @Nullable Predicate<C> predicate) {
        Assert.notNull(type, "'type' must not null.");
        final int finalPage = (page == null || page <= 0) ? 1 : page;
        final int finalSize = (size == null || size <= 0) ? 5 : size;
        Custom annotation = type.getAnnotation(Custom.class);

        List<CustomEntity> customEntities = repository.findAllByGroupAndVersionAndKind(
            annotation.group(), annotation.version(), annotation.kind(),
            PageRequest.of(finalPage - 1, finalSize)
        ).toStream().toList();

        List<C> customList = new ArrayList<C>();
        for (CustomEntity customEntity : customEntities) {
            UUID customEntityId = customEntity.getId();
            List<CustomMetadataEntity> customMetadataEntities =
                metadataRepository.findAllByCustomId(customEntityId).toStream().toList();
            CustomDto customDto = new CustomDto(customEntity, customMetadataEntities);
            C custom = CustomConverter.convertFrom(type, customDto);
            customList.add(custom);
        }

        customList =
            customList.stream().filter(Objects.isNull(predicate) ? Predicates.isTrue() : predicate)
                .toList();

        Long total = repository.countCustomEntitiesByGroupAndVersionAndKind(
            annotation.group(), annotation.version(), annotation.kind()
        ).block(BLOCK_TIMEOUT);

        if (total == null) {
            total = 0L;
        }

        return new PagingWrap<C>(finalPage, finalSize, total, customList);
    }

    @Override
    public <C> List<C> findAll(@NotNull Class<C> type, @Nullable Predicate<C> predicate) {
        Assert.notNull(type, "'type' must not null.");
        Custom annotation = type.getAnnotation(Custom.class);

        List<CustomEntity> customEntities = repository.findAllByGroupAndVersionAndKind(
            annotation.group(), annotation.version(), annotation.kind()
        ).toStream().toList();

        List<C> customList = new ArrayList<C>();
        for (CustomEntity customEntity : customEntities) {
            List<CustomMetadataEntity> customMetadataEntities =
                metadataRepository.findAllByCustomId(customEntity.getId())
                    .toStream().toList();
            CustomDto customDto = new CustomDto(customEntity, customMetadataEntities);
            C custom = CustomConverter.convertFrom(type, customDto);
            customList.add(custom);
        }

        customList =
            customList.stream().filter(Objects.isNull(predicate) ? Predicates.isTrue() : predicate)
                .toList();

        return customList;
    }
}
