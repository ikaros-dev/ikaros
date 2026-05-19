package run.ikaros.server.custom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Example;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.store.entity.CustomEntity;
import run.ikaros.server.store.entity.CustomMetadataEntity;
import run.ikaros.server.store.repository.CustomMetadataRepository;
import run.ikaros.server.store.repository.CustomRepository;

class ReactiveCustomClientImplTest {

    @Mock
    private CustomRepository customRepository;
    @Mock
    private CustomMetadataRepository metadataRepository;

    private ReactiveCustomClientImpl client;

    private UUID customId;
    private UUID metadataId;
    private String customName;
    private CustomEntity savedEntity;
    private CustomMetadataEntity savedMetadata;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        client = new ReactiveCustomClientImpl(customRepository, metadataRepository);

        customId = UUID.randomUUID();
        metadataId = UUID.randomUUID();
        customName = "test-custom";

        savedEntity = CustomEntity.builder()
            .id(customId)
            .group(DemoCustom.GROUP)
            .version(DemoCustom.VERSION)
            .kind(DemoCustom.KIND)
            .name(customName)
            .build();

        savedMetadata = CustomMetadataEntity.builder()
            .id(metadataId)
            .customId(customId)
            .key("title")
            .value("test-custom".getBytes())
            .build();
    }

    @Test
    void constructorInjectsRepositories() throws NoSuchFieldException, IllegalAccessException {
        java.lang.reflect.Field repoField =
            ReactiveCustomClientImpl.class.getDeclaredField("repository");
        repoField.setAccessible(true);
        Object repoValue = repoField.get(client);

        java.lang.reflect.Field metaRepoField =
            ReactiveCustomClientImpl.class.getDeclaredField("metadataRepository");
        metaRepoField.setAccessible(true);
        Object metaRepoValue = metaRepoField.get(client);

        assertThat(repoValue).isSameAs(customRepository);
        assertThat(metaRepoValue).isSameAs(metadataRepository);
    }

    @Test
    void createConvertsAndSavesCustom() {
        DemoCustom demoCustom = new DemoCustom()
            .setTitle(customName)
            .setNumber(42L);

        when(customRepository.insert(any(CustomEntity.class)))
            .thenAnswer(invocation -> {
                CustomEntity entity = invocation.getArgument(0);
                entity.setId(customId);
                return Mono.just(entity);
            });
        when(metadataRepository.insert(any(CustomMetadataEntity.class)))
            .thenAnswer(invocation -> {
                CustomMetadataEntity meta = invocation.getArgument(0);
                meta.setId(metadataId);
                return Mono.just(meta);
            });

        StepVerifier.create(client.create(demoCustom))
            .assertNext(result -> {
                assertThat(result).isInstanceOf(DemoCustom.class);
                DemoCustom created = (DemoCustom) result;
                assertThat(created.getTitle()).isEqualTo(customName);
            })
            .verifyComplete();
    }

    @Test
    void findOneFindsByTypeAndName() {
        when(customRepository.findOne(any(Example.class)))
            .thenReturn(Mono.just(savedEntity));
        when(metadataRepository.findAll(any(Example.class)))
            .thenReturn(Flux.just(savedMetadata));

        StepVerifier.create(client.findOne(DemoCustom.class, customName))
            .assertNext(result -> {
                assertThat(result).isInstanceOf(DemoCustom.class);
                DemoCustom found = (DemoCustom) result;
                assertThat(found.getTitle()).isEqualTo(customName);
            })
            .verifyComplete();
    }

    @Test
    void deleteRemovesCustom() {
        when(customRepository.findOne(any(Example.class)))
            .thenReturn(Mono.just(savedEntity));
        when(customRepository.delete(any(CustomEntity.class)))
            .thenReturn(Mono.empty());
        when(metadataRepository.deleteAllByCustomId(customId))
            .thenReturn(Mono.empty());

        DemoCustom demoCustom = new DemoCustom().setTitle(customName);

        StepVerifier.create(client.delete(demoCustom))
            .assertNext(result -> {
                assertThat(result).isInstanceOf(DemoCustom.class);
                assertThat(((DemoCustom) result).getTitle()).isEqualTo(customName);
            })
            .verifyComplete();
    }

    @Test
    void deleteAllDeletesAllMetadataThenAllCustoms() {
        when(metadataRepository.deleteAll()).thenReturn(Mono.empty());
        when(customRepository.deleteAll()).thenReturn(Mono.empty());

        StepVerifier.create(client.deleteAll())
            .verifyComplete();
    }
}
