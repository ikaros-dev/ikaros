package run.ikaros.server.core.binding.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.subject.service.SubjectSyncService;

class FetchAndCreateSubjectStepTest {

    @Mock
    private SubjectSyncService subjectSyncService;
    @Mock
    private SubjectService subjectService;
    private FetchAndCreateSubjectStep step;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        step = new FetchAndCreateSubjectStep(subjectSyncService, subjectService);
    }

    @Test
    void execute_syncsAndSetsSubjectId() {
        UUID directoryId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        SubjectSync syncRecord = SubjectSync.builder()
            .id(UUID.randomUUID())
            .subjectId(subjectId)
            .platform(SubjectSyncPlatform.BGM_TV)
            .platformId("12345")
            .build();

        when(subjectSyncService.sync(null, SubjectSyncPlatform.BGM_TV, "12345"))
            .thenReturn(Mono.empty());
        when(subjectSyncService.findSubjectSyncsByPlatformAndPlatformId(
            SubjectSyncPlatform.BGM_TV, "12345"))
            .thenReturn(Flux.just(syncRecord));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            directoryId, "Test Anime", SubjectSyncPlatform.BGM_TV);
        context.setPlatformId("12345");

        StepVerifier.create(step.execute(context))
            .assertNext(ctx -> {
                assertThat(ctx.getSubjectId()).isEqualTo(subjectId);
                assertThat(ctx.getSubjectSync()).isNotNull();
                assertThat(ctx.getSubjectSync().getPlatformId()).isEqualTo("12345");
            })
            .verifyComplete();
    }

    @Test
    void shouldSkip_whenPlatformIdIsNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setPlatformId(null);
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenPlatformIsNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", null);
        context.setPlatformId("12345");
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenBothAreNull() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", null);
        context.setPlatformId(null);
        assertThat(step.shouldSkip(context)).isTrue();
    }

    @Test
    void shouldSkip_whenBothAreSet() {
        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setPlatformId("12345");
        assertThat(step.shouldSkip(context)).isFalse();
    }

    @Test
    void rollback_removesSyncAndSubject() {
        UUID subjectId = UUID.randomUUID();
        SubjectSync syncRecord = SubjectSync.builder()
            .id(UUID.randomUUID()).subjectId(subjectId)
            .platform(SubjectSyncPlatform.BGM_TV).platformId("12345")
            .build();

        when(subjectSyncService.remove(syncRecord)).thenReturn(Mono.empty());
        when(subjectService.deleteById(subjectId)).thenReturn(Mono.empty());

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setSubjectSync(syncRecord);

        StepVerifier.create(step.rollback(context)).verifyComplete();

        verify(subjectSyncService).remove(syncRecord);
        verify(subjectService).deleteById(subjectId);
    }

    @Test
    void rollback_noSubjectSync_onlyRemovesSubject() {
        UUID subjectId = UUID.randomUUID();
        when(subjectService.deleteById(subjectId)).thenReturn(Mono.empty());

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setSubjectSync(null);

        StepVerifier.create(step.rollback(context)).verifyComplete();

        verify(subjectService).deleteById(subjectId);
    }

    @Test
    void rollback_noSubjectId_onlyRemovesSync() {
        SubjectSync syncRecord = SubjectSync.builder()
            .id(UUID.randomUUID()).subjectId(UUID.randomUUID())
            .platform(SubjectSyncPlatform.BGM_TV).platformId("12345")
            .build();

        when(subjectSyncService.remove(syncRecord)).thenReturn(Mono.empty());

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(null);
        context.setSubjectSync(syncRecord);

        StepVerifier.create(step.rollback(context)).verifyComplete();
        verify(subjectSyncService).remove(syncRecord);
    }

    @Test
    void rollback_errorInRemove_doesNotPropagate() {
        UUID subjectId = UUID.randomUUID();
        SubjectSync syncRecord = SubjectSync.builder()
            .id(UUID.randomUUID()).subjectId(subjectId)
            .platform(SubjectSyncPlatform.BGM_TV).platformId("12345")
            .build();

        when(subjectSyncService.remove(syncRecord))
            .thenReturn(Mono.error(new RuntimeException("Remove failed")));
        when(subjectService.deleteById(subjectId))
            .thenReturn(Mono.error(new RuntimeException("Delete failed")));

        DirectoryBindingContext context = DirectoryBindingContext.create(
            UUID.randomUUID(), "Test", SubjectSyncPlatform.BGM_TV);
        context.setSubjectId(subjectId);
        context.setSubjectSync(syncRecord);

        // Rollback swallows errors — should still complete
        StepVerifier.create(step.rollback(context)).verifyComplete();
    }

    @Test
    void order_is30() {
        assertThat(step.order()).isEqualTo(30);
    }

    @Test
    void name_isFetchAndCreateSubject() {
        assertThat(step.name()).isEqualTo("FetchAndCreateSubject");
    }
}
