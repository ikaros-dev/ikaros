package run.ikaros.server.core.meta.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subject.SubjectRecord;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.meta.MetaInfoService;

class MetaInfoEndpointTest {

    @Mock
    private MetaInfoService service;
    private MetaInfoEndpoint endpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        endpoint = new MetaInfoEndpoint(service);
    }

    @Test
    void endpoint_returnsRouterFunction() {
        RouterFunction<ServerResponse> routes = endpoint.endpoint();
        assertThat(routes).isNotNull();
    }

    @Test
    void searchSubjects_delegatesToService() {
        SubjectRecord record = new SubjectRecord(
            null, List.of(), List.of(), List.of(), Map.of());
        when(service.searchSubjects(SubjectSyncPlatform.BGM_TV, "Test"))
            .thenReturn(Flux.just(record));

        Flux<SubjectRecord> result =
            service.searchSubjects(SubjectSyncPlatform.BGM_TV, "Test");
        assertThat(result.collectList().block()).hasSize(1);
    }

    @Test
    void getSubjectByPlatformId_delegatesToService() {
        SubjectRecord record = new SubjectRecord(
            null, List.of(), List.of(), List.of(), Map.of());
        when(service.getSubjectByPlatformId(SubjectSyncPlatform.BGM_TV, "12345"))
            .thenReturn(Mono.just(record));

        SubjectRecord result =
            service.getSubjectByPlatformId(SubjectSyncPlatform.BGM_TV, "12345").block();
        assertThat(result).isNotNull();
    }
}
