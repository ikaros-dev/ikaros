package run.ikaros.server.core.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import run.ikaros.api.core.meta.DelegateMetaService;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.server.plugin.ExtensionComponentsFinder;

class DefaultMetaServiceTest {

    @Mock
    private ExtensionComponentsFinder extensionComponentsFinder;
    @Mock
    private DelegateMetaService delegate1;
    @Mock
    private DelegateMetaService delegate2;

    private DefaultMetaService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefaultMetaService(extensionComponentsFinder);
    }

    @Test
    void findOne_returnsFirstResult() {
        Subject subject1 = Subject.builder().name("Anime A").build();
        Subject subject2 = Subject.builder().name("Anime B").build();

        when(delegate1.findAll("Test"))
            .thenReturn(Flux.just(subject1, subject2));
        when(extensionComponentsFinder.getExtensions(DelegateMetaService.class))
            .thenReturn(List.of(delegate1));

        StepVerifier.create(service.findOne("Test"))
            .assertNext(s -> assertThat(s.getName()).isEqualTo("Anime A"))
            .verifyComplete();
    }

    @Test
    void findOne_noResults() {
        when(delegate1.findAll("Unknown"))
            .thenReturn(Flux.empty());
        when(extensionComponentsFinder.getExtensions(DelegateMetaService.class))
            .thenReturn(List.of(delegate1));

        StepVerifier.create(service.findOne("Unknown"))
            .verifyComplete();
    }

    @Test
    void findOne_blankKeyword_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.findOne(""));
    }

    @Test
    void findAll_aggregatesFromMultipleDelegates() {
        Subject subject1 = Subject.builder().name("Anime A").build();
        Subject subject2 = Subject.builder().name("Anime B").build();
        Subject subject3 = Subject.builder().name("Anime C").build();

        when(delegate1.findAll("Test"))
            .thenReturn(Flux.just(subject1, subject2));
        when(delegate2.findAll("Test"))
            .thenReturn(Flux.just(subject3));
        when(extensionComponentsFinder.getExtensions(DelegateMetaService.class))
            .thenReturn(List.of(delegate1, delegate2));

        StepVerifier.create(service.findAll("Test").collectList())
            .assertNext(list -> {
                assertThat(list).hasSize(3);
                assertThat(list).extracting(Subject::getName)
                    .containsExactly("Anime A", "Anime B", "Anime C");
            })
            .verifyComplete();
    }

    @Test
    void findAll_singleDelegate() {
        Subject subject1 = Subject.builder().name("Anime A").build();

        when(delegate1.findAll("Test"))
            .thenReturn(Flux.just(subject1));
        when(extensionComponentsFinder.getExtensions(DelegateMetaService.class))
            .thenReturn(List.of(delegate1));

        StepVerifier.create(service.findAll("Test").collectList())
            .assertNext(list -> {
                assertThat(list).hasSize(1);
                assertThat(list.get(0).getName()).isEqualTo("Anime A");
            })
            .verifyComplete();
    }

    @Test
    void findAll_noDelegates() {
        when(extensionComponentsFinder.getExtensions(DelegateMetaService.class))
            .thenReturn(List.of());

        StepVerifier.create(service.findAll("Test").collectList())
            .assertNext(list -> assertThat(list).isEmpty())
            .verifyComplete();
    }

    @Test
    void findAll_blankKeyword_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.findAll(""));
    }

    @Test
    void findAll_delegateReturnsEmpty() {
        when(delegate1.findAll("Test"))
            .thenReturn(Flux.empty());
        when(extensionComponentsFinder.getExtensions(DelegateMetaService.class))
            .thenReturn(List.of(delegate1));

        StepVerifier.create(service.findAll("Test").collectList())
            .assertNext(list -> assertThat(list).isEmpty())
            .verifyComplete();
    }

    @Test
    void findOne_prefersFirstOverSecondWhenMultipleDelegates() {
        Subject subject1 = Subject.builder().name("First Delegate Result").build();
        Subject subject2 = Subject.builder().name("Second Delegate Result").build();

        when(delegate1.findAll("Test"))
            .thenReturn(Flux.just(subject1));
        when(delegate2.findAll("Test"))
            .thenReturn(Flux.just(subject2));
        when(extensionComponentsFinder.getExtensions(DelegateMetaService.class))
            .thenReturn(List.of(delegate1, delegate2));

        StepVerifier.create(service.findOne("Test"))
            .assertNext(s -> assertThat(s.getName()).isEqualTo("First Delegate Result"))
            .verifyComplete();
    }
}
