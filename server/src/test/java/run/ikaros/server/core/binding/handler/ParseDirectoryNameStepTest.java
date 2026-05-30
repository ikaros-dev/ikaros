package run.ikaros.server.core.binding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

class ParseDirectoryNameStepTest {

    private ParseDirectoryNameStep step;

    @BeforeEach
    void setUp() {
        step = new ParseDirectoryNameStep();
    }

    @Test
    void execute_parsesBracketsAndCleanName() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "[SubGroup] Anime Title [1080p][HEVC]",
            SubjectSyncPlatform.BGM_TV);

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getBracketTags())
                    .containsExactly("SubGroup", "1080p", "HEVC");
                assertThat(ctx.getCleanName()).isEqualTo("Anime Title");
            })
            .verifyComplete();
    }

    @Test
    void execute_noBrackets() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Plain Anime Title", SubjectSyncPlatform.BGM_TV);

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getBracketTags()).isEmpty();
                assertThat(ctx.getCleanName()).isEqualTo("Plain Anime Title");
            })
            .verifyComplete();
    }

    @Test
    void execute_emptyDirectoryName_throwsException() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "", SubjectSyncPlatform.BGM_TV);
        // RegexUtils.getFileTag("") calls AssertUtils.notBlank which throws synchronously
        assertThrows(IllegalArgumentException.class,
            () -> step.execute(context));
    }

    @Test
    void execute_multipleBracketGroups() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "[Tag1][Tag2][Tag3] Title Here",
            SubjectSyncPlatform.BGM_TV);

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getBracketTags())
                    .containsExactly("Tag1", "Tag2", "Tag3");
                assertThat(ctx.getCleanName()).isEqualTo("Title Here");
            })
            .verifyComplete();
    }

    @Test
    void execute_nestedBrackets_extractsInnerBrackets() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "[[Nested]] Title", SubjectSyncPlatform.BGM_TV);

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                // Inner [Nested] is matched; outer brackets remain as `[] Title`
                assertThat(ctx.getBracketTags()).containsExactly("Nested");
                assertThat(ctx.getCleanName()).isEqualTo("[] Title");
            })
            .verifyComplete();
    }

    @Test
    void order_is10() {
        assertThat(step.order()).isEqualTo(10);
    }

    @Test
    void name_isParseDirectoryName() {
        assertThat(step.name()).isEqualTo("ParseDirectoryName");
    }

    @Test
    void rollback_doesNothing() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        StepVerifier.create(step.rollback(context)).verifyComplete();
    }
}
