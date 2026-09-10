package run.ikaros.operations.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/health/operations")
public class OperationsDiagnosticsController {
    private final OperationsDiagnosticsService diagnostics;

    public OperationsDiagnosticsController(OperationsDiagnosticsService diagnostics) {
        this.diagnostics = diagnostics;
    }

    @GetMapping
    public Mono<OperationsDiagnosticsView> get() {
        return diagnostics.get();
    }
}
