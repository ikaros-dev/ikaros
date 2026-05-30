package run.ikaros.server.core.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.subject.Subject;

class MetaOperatorTest {

    @Mock
    private MetaService metaService;
    private MetaOperator operator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        operator = new MetaOperator(metaService);
    }

    @Test
    void findOne_delegatesToMetaService() {
        Subject expected = Subject.builder().name("Test Anime").build();
        when(metaService.findOne("Test Anime")).thenReturn(Mono.just(expected));

        StepVerifier.create(operator.findOne("Test Anime"))
            .assertNext(subject ->
                assertThat(subject.getName()).isEqualTo("Test Anime")
            )
            .verifyComplete();
    }

    @Test
    void findOne_notFound() {
        when(metaService.findOne("Unknown")).thenReturn(Mono.empty());

        StepVerifier.create(operator.findOne("Unknown"))
            .verifyComplete();
    }

    @Test
    void findAll_delegatesToMetaService() {
        Subject subject1 = Subject.builder().name("Anime A").build();
        Subject subject2 = Subject.builder().name("Anime B").build();

        when(metaService.findAll("Anime")).thenReturn(Flux.just(subject1, subject2));

        StepVerifier.create(operator.findAll("Anime").collectList())
            .assertNext(list -> assertThat(list).hasSize(2))
            .verifyComplete();
    }

    @Test
    void findAll_emptyResult() {
        when(metaService.findAll("Unknown")).thenReturn(Flux.empty());

        StepVerifier.create(operator.findAll("Unknown").collectList())
            .assertNext(list -> assertThat(list).isEmpty())
            .verifyComplete();
    }
}
