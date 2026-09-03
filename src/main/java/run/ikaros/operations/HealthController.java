package run.ikaros.operations;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/** Minimal liveness/readiness probes for the server runtime. */
@RestController
@RequestMapping("/api/health")
public class HealthController {
    private final DatabaseClient database;

    public HealthController(DatabaseClient database) {
        this.database = database;
    }

    @GetMapping("/live")
    public Mono<ResponseEntity<Map<String, String>>> live() {
        return Mono.just(ResponseEntity.ok(Map.of("status", "UP")));
    }

    @GetMapping("/ready")
    public Mono<ResponseEntity<Map<String, String>>> ready() {
        return database.sql("select 1").fetch().rowsUpdated()
            .map(ignored -> ResponseEntity.ok(Map.of("status", "UP")))
            .onErrorResume(error -> Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "DOWN"))));
    }
}
