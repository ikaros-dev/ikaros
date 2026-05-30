package run.ikaros.server.core.episode.sequence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.episode.EpisodeSequenceRegular;
import run.ikaros.api.core.episode.EpisodeSequenceRegularResult;
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.store.entity.EpisodeSequenceRegularEntity;
import run.ikaros.server.store.repository.EpisodeSequenceRegularRepository;

class EpisodeSequenceRegularServiceImplTest {

    @Mock
    private EpisodeSequenceRegularRepository repository;
    @Mock
    private EpisodeSequenceRegularChain chain;
    private EpisodeSequenceRegularServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EpisodeSequenceRegularServiceImpl(repository, chain);
    }

    @Test
    void save_insertWhenIdIsNull() {
        EpisodeSequenceRegular regular = EpisodeSequenceRegular.builder()
            .name("OP Rule").regex("NCOP")
            .epGroup(EpisodeGroup.OPENING_SONG).priority(100).enabled(true).build();

        when(repository.insert(any(EpisodeSequenceRegularEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.save(regular))
            .assertNext(saved -> {
                assertThat(saved.getName()).isEqualTo("OP Rule");
                assertThat(saved.getRegex()).isEqualTo("NCOP");
                assertThat(saved.getEpGroup()).isEqualTo(EpisodeGroup.OPENING_SONG);
            })
            .verifyComplete();
    }

    @Test
    void save_updateWhenIdExists() {
        UUID id = UUID.randomUUID();
        EpisodeSequenceRegular regular = EpisodeSequenceRegular.builder()
            .id(id).name("Updated Rule").regex("SP")
            .epGroup(EpisodeGroup.SPECIAL_PROMOTION).priority(50).enabled(true).build();

        when(repository.update(any(EpisodeSequenceRegularEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.save(regular))
            .assertNext(saved -> {
                assertThat(saved.getId()).isEqualTo(id);
                assertThat(saved.getName()).isEqualTo("Updated Rule");
            })
            .verifyComplete();
    }

    @Test
    void removeById_deletesFromRepository() {
        UUID id = UUID.randomUUID();
        when(repository.deleteById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.removeById(id))
            .verifyComplete();

        verify(repository).deleteById(id);
    }

    @Test
    void findById_returnsRule() {
        UUID id = UUID.randomUUID();
        EpisodeSequenceRegularEntity entity = EpisodeSequenceRegularEntity.builder()
            .name("ED Rule").regex("NCED")
            .epGroup(EpisodeGroup.ENDING_SONG).priority(90).enabled(true).build();
        entity.setId(id);

        when(repository.findById(id)).thenReturn(Mono.just(entity));

        StepVerifier.create(service.findById(id))
            .assertNext(found -> {
                assertThat(found.getId()).isEqualTo(id);
                assertThat(found.getName()).isEqualTo("ED Rule");
            })
            .verifyComplete();
    }

    @Test
    void findById_notFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Mono.empty());

        StepVerifier.create(service.findById(id))
            .verifyComplete();
    }

    @Test
    void findAll_returnsPagedResults() {
        EpisodeSequenceRegularEntity entity1 = EpisodeSequenceRegularEntity.builder()
            .name("Rule1").regex("regex1").enabled(true).build();
        entity1.setId(UUID.randomUUID());
        EpisodeSequenceRegularEntity entity2 = EpisodeSequenceRegularEntity.builder()
            .name("Rule2").regex("regex2").enabled(true).build();
        entity2.setId(UUID.randomUUID());

        when(repository.findAllByEnabledTrueOrderByPriorityDesc(any(Pageable.class)))
            .thenReturn(Flux.just(entity1, entity2));
        when(repository.countAllByEnabledTrue()).thenReturn(Mono.just(2L));

        StepVerifier.create(service.findAll(1, 10))
            .assertNext(paging -> {
                assertThat(paging.getPage()).isEqualTo(1);
                assertThat(paging.getSize()).isEqualTo(10);
                assertThat(paging.getTotal()).isEqualTo(2L);
                assertThat(paging.getItems()).hasSize(2);
            })
            .verifyComplete();
    }

    @Test
    void findAll_usesDefaultsWhenNull() {
        when(repository.findAllByEnabledTrueOrderByPriorityDesc(any(Pageable.class)))
            .thenReturn(Flux.empty());
        when(repository.countAllByEnabledTrue()).thenReturn(Mono.just(0L));

        StepVerifier.create(service.findAll(null, null))
            .assertNext(paging -> {
                assertThat(paging.getPage()).isEqualTo(1);
                assertThat(paging.getSize()).isEqualTo(10);
                assertThat(paging.getTotal()).isEqualTo(0L);
            })
            .verifyComplete();
    }

    @Test
    void match_delegatesToChain() {
        EpisodeSequenceRegularResult expected = EpisodeSequenceRegularResult.builder()
            .matched(true).attachmentName("NCOP.mkv")
            .epGroup(EpisodeGroup.OPENING_SONG).build();

        when(chain.match("NCOP.mkv")).thenReturn(Mono.just(expected));

        StepVerifier.create(service.match("NCOP.mkv"))
            .assertNext(result -> {
                assertThat(result.isMatched()).isTrue();
                assertThat(result.getEpGroup()).isEqualTo(EpisodeGroup.OPENING_SONG);
            })
            .verifyComplete();
    }
}
