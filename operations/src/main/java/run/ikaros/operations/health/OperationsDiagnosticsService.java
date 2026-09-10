package run.ikaros.operations.health;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.integration.api.DurableEventDeliveryDiagnostics;
import run.ikaros.integration.api.DurableEventDeliveryStatus;
import run.ikaros.operations.task.BackgroundTaskRepository;

@Service
public class OperationsDiagnosticsService {
    private final BackgroundTaskRepository tasks;
    private final DurableEventDeliveryDiagnostics delivery;

    public OperationsDiagnosticsService(BackgroundTaskRepository tasks, DurableEventDeliveryDiagnostics delivery) {
        this.tasks = tasks; this.delivery = delivery;
    }

    public Mono<OperationsDiagnosticsView> get() {
        return Mono.zip(tasks.countByStatus("PENDING"), tasks.countByStatus("RUNNING"),
                tasks.countByStatus("FAILED"), tasks.countByStatus("TIMED_OUT"), delivery.status())
            .map(value -> new OperationsDiagnosticsView(value.getT1(), value.getT2(), value.getT3(), value.getT4(), value.getT5()));
    }
}
