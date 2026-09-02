package run.ikaros.planning;
import java.util.UUID; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono;
public interface OkrService { Mono<OkrCycleView> createCycle(UUID o,CreateOkrCycleRequest r); Flux<OkrCycleView> cycles(UUID o); Mono<OkrObjectiveView> createObjective(UUID o,CreateOkrObjectiveRequest r); Flux<OkrObjectiveView> objectives(UUID o,UUID c); Mono<OkrKeyResultView> createKeyResult(UUID o,UUID objective,CreateOkrKeyResultRequest r); Flux<OkrKeyResultView> keyResults(UUID o,UUID objective); }
