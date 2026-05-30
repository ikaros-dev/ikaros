package run.ikaros.server.core.binding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectRecord;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.meta.MetaInfoService;

class FindSubjectInfoStepTest {

    @Mock
    private MetaInfoService metaInfoService;
    private FindSubjectInfoStep step;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        step = new FindSubjectInfoStep(metaInfoService);
    }

    @Test
    void execute_findsSubjectByCleanName() {
        UUID subjectId = UUID.randomUUID();
        SubjectSync sync = SubjectSync.builder()
            .id(UUID.randomUUID()).subjectId(subjectId)
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId("12345")
            .build();
        Subject subject = Subject.builder()
            .id(subjectId).name("Test Anime").build();
        SubjectRecord record = new SubjectRecord(
            subject, List.of(), List.of(), List.of(sync), Map.of());

        when(metaInfoService.searchSubjects(SubjectSyncPlatform.BGM_TV, "Test Anime"))
            .thenReturn(Flux.just(record));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test Anime", SubjectSyncPlatform.BGM_TV);
        context.setCleanName("Test Anime");

        StepVerifier.create(step.execute(context))
            .assertNext(ctx ->
                assertThat(ctx.getPlatformId()).isEqualTo("12345")
            )
            .verifyComplete();
    }

    @Test
    void execute_usesKeywordOverCleanName() {
        UUID subjectId = UUID.randomUUID();
        SubjectSync sync = SubjectSync.builder()
            .id(UUID.randomUUID()).subjectId(subjectId)
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId("67890")
            .build();
        Subject subject = Subject.builder()
            .id(subjectId).name("Search Result").build();
        SubjectRecord record = new SubjectRecord(
            subject, List.of(), List.of(), List.of(sync), Map.of());

        when(metaInfoService.searchSubjects(SubjectSyncPlatform.BGM_TV, "Custom Keyword"))
            .thenReturn(Flux.just(record));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test Anime", SubjectSyncPlatform.BGM_TV);
        context.setCleanName("Test Anime");
        context.setKeyword("Custom Keyword");

        StepVerifier.create(step.execute(context))
            .assertNext(ctx ->
                assertThat(ctx.getPlatformId()).isEqualTo("67890")
            )
            .verifyComplete();
    }

    @Test
    void execute_noResultsFound() {
        when(metaInfoService.searchSubjects(eq(SubjectSyncPlatform.BGM_TV), any()))
            .thenReturn(Flux.empty());

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Unknown Anime", SubjectSyncPlatform.BGM_TV);
        context.setCleanName("Unknown Anime");

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> assertThat(ctx.getPlatformId()).isNull())
            .verifyComplete();
    }

    @Test
    void execute_subjectRecordWithNoSyncs() {
        Subject subject = Subject.builder()
            .id(UUID.randomUUID()).name("No Sync").build();
        SubjectRecord record = new SubjectRecord(
            subject, List.of(), List.of(), List.of(), Map.of());

        when(metaInfoService.searchSubjects(SubjectSyncPlatform.BGM_TV, "No Sync"))
            .thenReturn(Flux.just(record));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "No Sync", SubjectSyncPlatform.BGM_TV);
        context.setCleanName("No Sync");

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> assertThat(ctx.getPlatformId()).isNull())
            .verifyComplete();
    }

    @Test
    void shouldSkip_whenPlatformIsNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", null);
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenPlatformIsSet() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        assertThat(step.shouldSkip(context)).isFalse();
    }

    @Test
    void order_is20() {
        assertThat(step.order()).isEqualTo(20);
    }

    @Test
    void name_isFindSubjectInfo() {
        assertThat(step.name()).isEqualTo("FindSubjectInfo");
    }

    @Test
    void rollback_doesNothing() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        StepVerifier.create(step.rollback(context)).verifyComplete();
    }
}
