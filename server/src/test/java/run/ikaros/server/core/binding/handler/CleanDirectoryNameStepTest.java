package run.ikaros.server.core.binding.handler;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

class CleanDirectoryNameStepTest {

    private CleanDirectoryNameStep step;

    @BeforeEach
    void setUp() {
        step = new CleanDirectoryNameStep();
    }

    @Test
    void execute_removesSpecialCharacters() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Dummy", SubjectSyncPlatform.BGM_TV);
        // regex does NOT include — (em-dash), [, or ]
        context.setCleanName("Attack on Titan!! Season 2 — [1080p]");

        StepVerifier.create(step.execute(context))
            .assertNext(ctx ->
                assertThat(ctx.getCleanName()).isEqualTo("Attack on Titan Season 2 — [1080p]")
            )
            .verifyComplete();
    }

    @Test
    void execute_removesWordsWithApostrophe() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Dummy", SubjectSyncPlatform.BGM_TV);
        // "Kuroko's" contains apostrophe, filtered out entirely
        context.setCleanName("Kuroko's Basketball S3");

        StepVerifier.create(step.execute(context))
            .assertNext(ctx ->
                assertThat(ctx.getCleanName()).isEqualTo("Basketball S3")
            )
            .verifyComplete();
    }

    @Test
    void execute_removesChinesePunctuation() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Dummy", SubjectSyncPlatform.BGM_TV);
        context.setCleanName("进击的巨人：最终季 第2部分");

        StepVerifier.create(step.execute(context))
            .assertNext(ctx ->
                assertThat(ctx.getCleanName()).isEqualTo("进击的巨人最终季 第2部分")
            )
            .verifyComplete();
    }

    @Test
    void execute_noSpecialCharacters() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Dummy", SubjectSyncPlatform.BGM_TV);
        context.setCleanName("Steins Gate");

        StepVerifier.create(step.execute(context))
            .assertNext(ctx ->
                assertThat(ctx.getCleanName()).isEqualTo("Steins Gate")
            )
            .verifyComplete();
    }

    @Test
    void shouldSkip_whenCleanNameIsNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setCleanName(null);
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenCleanNameIsBlank() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setCleanName("   ");
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenCleanNameIsValid() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setCleanName("Valid Name");
        assertThat(step.shouldSkip(context)).isFalse();
    }

    @Test
    void order_is15() {
        assertThat(step.order()).isEqualTo(15);
    }

    @Test
    void name_isCleanDirectoryName() {
        assertThat(step.name()).isEqualTo("CleanDirectoryName");
    }

    @Test
    void rollback_doesNothing() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        StepVerifier.create(step.rollback(context)).verifyComplete();
    }
}
