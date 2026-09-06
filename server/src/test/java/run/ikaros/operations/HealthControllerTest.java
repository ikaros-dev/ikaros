package run.ikaros.operations;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.FetchSpec;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class HealthControllerTest {
    @Test
    void livenessDoesNotRequireDatabase() {
        HealthController controller = new HealthController(null);

        StepVerifier.create(controller.live())
            .expectNextMatches(response -> response.getStatusCode().equals(HttpStatus.OK)
                && response.getBody().equals(Map.of("status", "UP")))
            .verifyComplete();
    }

    @Test
    void readinessReportsDatabaseFailure() {
        DatabaseClient database = mock(DatabaseClient.class);
        DatabaseClient.GenericExecuteSpec query = mock(DatabaseClient.GenericExecuteSpec.class);
        FetchSpec<Map<String, Object>> result = mock(FetchSpec.class);
        when(database.sql(anyString())).thenReturn(query);
        when(query.fetch()).thenReturn(result);
        when(result.one()).thenReturn(Mono.error(new IllegalStateException("database unavailable")));

        StepVerifier.create(new HealthController(database).ready())
            .expectNextMatches(response -> response.getStatusCode().equals(HttpStatus.SERVICE_UNAVAILABLE)
                && response.getBody().equals(Map.of("status", "DOWN")))
            .verifyComplete();
    }
}
