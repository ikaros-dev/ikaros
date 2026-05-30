package run.ikaros.server.core.binding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import run.ikaros.api.core.tag.Tag;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.TagType;
import run.ikaros.server.core.tag.TagService;

class CreateSubjectTagsStepTest {

    @Mock
    private TagService tagService;
    private CreateSubjectTagsStep step;
    private UUID subjectId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        step = new CreateSubjectTagsStep(tagService);
        subjectId = UUID.randomUUID();
    }

    @Test
    void execute_createsTagsFromBracketTags() {
        when(tagService.create(any(Tag.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "[1080p][SubGroup] Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setBracketTags(List.of("1080p", "SubGroup"));

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getCreatedTags()).hasSize(2);
                assertThat(ctx.getCreatedTags())
                    .extracting(Tag::getName)
                    .containsExactly("1080p", "SubGroup");
                assertThat(ctx.getCreatedTags())
                    .allMatch(tag -> tag.getType() == TagType.SUBJECT)
                    .allMatch(tag -> tag.getMasterId().equals(subjectId));
            })
            .verifyComplete();
    }

    @Test
    void shouldSkip_whenBracketTagsIsNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setBracketTags(null);
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenBracketTagsIsEmpty() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setBracketTags(List.of());
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenSubjectIdIsNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "[1080p] Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(null);
        context.setBracketTags(List.of("1080p"));
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenAllConditionsMet_returnsFalse() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "[1080p] Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setBracketTags(List.of("1080p"));
        assertThat(step.shouldSkip(context)).isFalse();
    }

    @Test
    void rollback_removesCreatedTags() {
        Tag tag1 = Tag.builder().id(UUID.randomUUID()).name("1080p").build();
        Tag tag2 = Tag.builder().id(UUID.randomUUID()).name("SubGroup").build();

        when(tagService.removeById(tag1.getId())).thenReturn(Mono.empty());
        when(tagService.removeById(tag2.getId())).thenReturn(Mono.empty());

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.getCreatedTags().add(tag1);
        context.getCreatedTags().add(tag2);

        StepVerifier.create(step.rollback(context)).verifyComplete();

        verify(tagService).removeById(tag1.getId());
        verify(tagService).removeById(tag2.getId());
    }

    @Test
    void rollback_error_doesNotPropagate() {
        Tag tag = Tag.builder().id(UUID.randomUUID()).name("1080p").build();
        when(tagService.removeById(tag.getId()))
            .thenReturn(Mono.error(new RuntimeException("Remove failed")));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.getCreatedTags().add(tag);

        StepVerifier.create(step.rollback(context)).verifyComplete();
    }

    @Test
    void rollback_noCreatedTags() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        StepVerifier.create(step.rollback(context)).verifyComplete();
    }

    @Test
    void order_is40() {
        assertThat(step.order()).isEqualTo(40);
    }

    @Test
    void name_isCreateSubjectTags() {
        assertThat(step.name()).isEqualTo("CreateSubjectTags");
    }
}
