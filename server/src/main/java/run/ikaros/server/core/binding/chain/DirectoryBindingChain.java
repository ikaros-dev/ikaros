package run.ikaros.server.core.binding.chain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.core.binding.DirectoryBindingStepStatus;

@Slf4j
public class DirectoryBindingChain {

    private final List<DirectoryBindingStep> steps;

    public DirectoryBindingChain(List<DirectoryBindingStep> steps) {
        this.steps = steps;
    }

    /**
     * Execute all steps in order. On failure, rollback completed steps in reverse.
     */
    public Mono<DirectoryBindingContext> execute(DirectoryBindingContext context) {
        List<DirectoryBindingStep> sorted = steps.stream()
            .sorted(Comparator.comparingInt(DirectoryBindingStep::order))
            .toList();

        Mono<DirectoryBindingContext> chain = Mono.just(context);

        for (DirectoryBindingStep step : sorted) {
            chain = chain.flatMap(ctx -> executeStep(ctx, step, sorted));
        }

        return chain;
    }

    private Mono<DirectoryBindingContext> executeStep(DirectoryBindingContext context,
                                                      DirectoryBindingStep step,
                                                      List<DirectoryBindingStep> allSteps) {
        if (step.shouldSkip(context)) {
            log.info("Step [{}] skipped.", step.name());
            context.getStepResults().put(step.name(), DirectoryBindingStepStatus.SUCCESS);
            return Mono.just(context);
        }

        log.info("Step [{}] running...", step.name());
        context.getStepResults().put(step.name(), DirectoryBindingStepStatus.RUNNING);

        return step.execute(context)
            .doOnSuccess(ctx -> {
                log.info("Step [{}] success.", step.name());
                ctx.getStepResults().put(step.name(), DirectoryBindingStepStatus.SUCCESS);
            })
            .onErrorResume(err -> {
                log.error("Step [{}] fail: {}", step.name(), err.getMessage(), err);
                context.getStepResults().put(step.name(), DirectoryBindingStepStatus.FAIL);
                context.getStepErrors().put(step.name(), err.getMessage());
                return rollbackCompletedSteps(context, allSteps)
                    .then(Mono.error(err));
            });
    }

    private Mono<Void> rollbackCompletedSteps(DirectoryBindingContext context,
                                              List<DirectoryBindingStep> allSteps) {
        List<DirectoryBindingStep> toRollback = new ArrayList<>();
        for (int i = allSteps.size() - 1; i >= 0; i--) {
            DirectoryBindingStep step = allSteps.get(i);
            if (context.getStepResults().get(step.name()) == DirectoryBindingStepStatus.SUCCESS) {
                toRollback.add(step);
            }
        }

        if (toRollback.isEmpty()) {
            return Mono.empty();
        }

        log.info("Rolling back {} completed steps...", toRollback.size());
        return Flux.fromIterable(toRollback)
            .flatMap(step -> step.rollback(context)
                .doOnSuccess(v -> {
                    log.info("Step [{}] rolled back.", step.name());
                    context.getStepResults()
                        .put(step.name(), DirectoryBindingStepStatus.ROLLED_BACK);
                })
                .onErrorResume(rollbackErr -> {
                    log.error("Rollback failed for step [{}]: {}",
                        step.name(), rollbackErr.getMessage(), rollbackErr);
                    return Mono.empty();
                }))
            .then();
    }
}
