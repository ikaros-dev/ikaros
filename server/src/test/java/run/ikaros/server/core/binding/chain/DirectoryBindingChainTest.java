package run.ikaros.server.core.binding.chain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.core.binding.DirectoryBindingStepStatus;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

class DirectoryBindingChainTest {

    @Mock
    private DirectoryBindingStep step1;
    @Mock
    private DirectoryBindingStep step2;
    @Mock
    private DirectoryBindingStep step3;

    private DirectoryBindingContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        context = DirectoryBindingContext.create(
            UUID.randomUUID(), "TestDir [1080p]", SubjectSyncPlatform.BGM_TV);
    }

    @Test
    void execute_allStepsSuccess() {
        when(step1.name()).thenReturn("Step1");
        when(step1.order()).thenReturn(10);
        when(step1.shouldSkip(any())).thenReturn(false);
        when(step1.execute(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(step2.name()).thenReturn("Step2");
        when(step2.order()).thenReturn(20);
        when(step2.shouldSkip(any())).thenReturn(false);
        when(step2.execute(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(step3.name()).thenReturn("Step3");
        when(step3.order()).thenReturn(30);
        when(step3.shouldSkip(any())).thenReturn(false);
        when(step3.execute(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        DirectoryBindingChain chain = new DirectoryBindingChain(List.of(step2, step1, step3));

        StepVerifier.create(chain.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getStepResults())
                    .containsEntry("Step1", DirectoryBindingStepStatus.SUCCESS)
                    .containsEntry("Step2", DirectoryBindingStepStatus.SUCCESS)
                    .containsEntry("Step3", DirectoryBindingStepStatus.SUCCESS);
            })
            .verifyComplete();
    }

    @Test
    void execute_withSkippedStep() {
        when(step1.name()).thenReturn("Step1");
        when(step1.order()).thenReturn(10);
        when(step1.shouldSkip(any())).thenReturn(true);
        when(step1.execute(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        when(step2.name()).thenReturn("Step2");
        when(step2.order()).thenReturn(20);
        when(step2.shouldSkip(any())).thenReturn(false);
        when(step2.execute(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        DirectoryBindingChain chain = new DirectoryBindingChain(List.of(step1, step2));

        StepVerifier.create(chain.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getStepResults())
                    .containsEntry("Step1", DirectoryBindingStepStatus.SUCCESS)
                    .containsEntry("Step2", DirectoryBindingStepStatus.SUCCESS);
            })
            .verifyComplete();
    }

    @Test
    void execute_stepFailure_triggersRollback() {
        when(step1.name()).thenReturn("Step1");
        when(step1.order()).thenReturn(10);
        when(step1.shouldSkip(any())).thenReturn(false);
        when(step1.execute(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(step1.rollback(any())).thenReturn(Mono.empty());

        when(step2.name()).thenReturn("Step2");
        when(step2.order()).thenReturn(20);
        when(step2.shouldSkip(any())).thenReturn(false);
        when(step2.execute(any()))
            .thenReturn(Mono.error(new RuntimeException("Step2 failed")));
        when(step2.rollback(any())).thenReturn(Mono.empty());

        DirectoryBindingChain chain = new DirectoryBindingChain(List.of(step1, step2));

        StepVerifier.create(chain.execute(context))
            .expectError(RuntimeException.class)
            .verify();

        assertThat(context.getStepResults())
            .containsEntry("Step1", DirectoryBindingStepStatus.ROLLED_BACK)
            .containsEntry("Step2", DirectoryBindingStepStatus.FAIL);
    }

    @Test
    void execute_emptyStepList() {
        DirectoryBindingChain chain = new DirectoryBindingChain(List.of());

        StepVerifier.create(chain.execute(context))
            .assertNext(ctx -> assertThat(ctx.getStepResults()).isEmpty())
            .verifyComplete();
    }

    @Test
    void execute_rollbackError_doesNotFailChain() {
        when(step1.name()).thenReturn("Step1");
        when(step1.order()).thenReturn(10);
        when(step1.shouldSkip(any())).thenReturn(false);
        when(step1.execute(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(step1.rollback(any()))
            .thenReturn(Mono.error(new RuntimeException("Rollback failed")));

        when(step2.name()).thenReturn("Step2");
        when(step2.order()).thenReturn(20);
        when(step2.shouldSkip(any())).thenReturn(false);
        when(step2.execute(any()))
            .thenReturn(Mono.error(new RuntimeException("Step2 failed")));
        when(step2.rollback(any())).thenReturn(Mono.empty());

        DirectoryBindingChain chain = new DirectoryBindingChain(List.of(step1, step2));

        StepVerifier.create(chain.execute(context))
            .expectError(RuntimeException.class)
            .verify();

        // Step1 stays as SUCCESS because doOnSuccess in rollback didn't fire
        // (the rollback error is swallowed by onErrorResume)
        assertThat(context.getStepResults())
            .containsEntry("Step1", DirectoryBindingStepStatus.SUCCESS);
    }
}
