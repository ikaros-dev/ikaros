package run.ikaros.server.core.meta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.meta.MetaInfoExtensionPoint;
import run.ikaros.api.core.subject.SubjectRecord;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.server.plugin.ExtensionComponentsFinder;

class DefaultMetaInfoServiceTest {

    @Mock
    private ExtensionComponentsFinder extensionComponentsFinder;
    @Mock
    private MetaInfoExtensionPoint bgmExtension;
    @Mock
    private MetaInfoExtensionPoint anotherExtension;

    private DefaultMetaInfoService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new DefaultMetaInfoService(extensionComponentsFinder);

        when(bgmExtension.getPlatform()).thenReturn(SubjectSyncPlatform.BGM_TV);
        when(anotherExtension.getPlatform()).thenReturn(SubjectSyncPlatform.BGM_TV);
    }

    private static SubjectRecord createRecord() {
        return new SubjectRecord(null, List.of(), List.of(), List.of(), Map.of());
    }

    @Test
    void searchSubjects_findsExtensionAndSearches() {
        SubjectRecord record = createRecord();

        when(bgmExtension.searchSubjects("Test"))
            .thenReturn(Flux.just(record));
        when(extensionComponentsFinder.getExtensions(MetaInfoExtensionPoint.class))
            .thenReturn(List.of(bgmExtension));

        StepVerifier.create(service.searchSubjects(SubjectSyncPlatform.BGM_TV, "Test"))
            .assertNext(r -> assertThat(r).isNotNull())
            .verifyComplete();
    }

    @Test
    void searchSubjects_noExtension_throwsException() {
        when(extensionComponentsFinder.getExtensions(MetaInfoExtensionPoint.class))
            .thenReturn(List.of());

        StepVerifier.create(service.searchSubjects(SubjectSyncPlatform.BGM_TV, "Test"))
            .expectError(IllegalStateException.class)
            .verify();
    }

    @Test
    void searchSubjects_multipleExtensions_usesFirstMatch() {
        SubjectRecord record = createRecord();

        when(bgmExtension.searchSubjects("Test"))
            .thenReturn(Flux.just(record));
        when(extensionComponentsFinder.getExtensions(MetaInfoExtensionPoint.class))
            .thenReturn(List.of(bgmExtension, anotherExtension));

        StepVerifier.create(service.searchSubjects(SubjectSyncPlatform.BGM_TV, "Test"))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void searchSubjects_returnsMultipleResults() {
        SubjectRecord record1 = createRecord();
        SubjectRecord record2 = createRecord();

        when(bgmExtension.searchSubjects("Test"))
            .thenReturn(Flux.just(record1, record2));
        when(extensionComponentsFinder.getExtensions(MetaInfoExtensionPoint.class))
            .thenReturn(List.of(bgmExtension));

        StepVerifier.create(service.searchSubjects(SubjectSyncPlatform.BGM_TV, "Test"))
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void searchSubjects_emptyResult() {
        when(bgmExtension.searchSubjects("Unknown"))
            .thenReturn(Flux.empty());
        when(extensionComponentsFinder.getExtensions(MetaInfoExtensionPoint.class))
            .thenReturn(List.of(bgmExtension));

        StepVerifier.create(service.searchSubjects(SubjectSyncPlatform.BGM_TV, "Unknown"))
            .verifyComplete();
    }

    @Test
    void searchSubjects_nullPlatform_throwsException() {
        // AssertUtils.notNull throws synchronously during assembly
        assertThrows(IllegalArgumentException.class,
            () -> service.searchSubjects(null, "Test"));
    }

    @Test
    void searchSubjects_blankKeyword_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.searchSubjects(SubjectSyncPlatform.BGM_TV, ""));
    }

    @Test
    void getSubjectByPlatformId_success() {
        SubjectSync sync = SubjectSync.builder().platformId("12345").build();
        SubjectRecord record = new SubjectRecord(
            null, List.of(), List.of(), List.of(sync), Map.of());

        when(bgmExtension.getSubjectByPlatformId("12345"))
            .thenReturn(Mono.just(record));
        when(extensionComponentsFinder.getExtensions(MetaInfoExtensionPoint.class))
            .thenReturn(List.of(bgmExtension));

        StepVerifier.create(
                service.getSubjectByPlatformId(SubjectSyncPlatform.BGM_TV, "12345"))
            .assertNext(r ->
                assertThat(r.syncs().get(0).getPlatformId()).isEqualTo("12345")
            )
            .verifyComplete();
    }

    @Test
    void getSubjectByPlatformId_notFound() {
        when(bgmExtension.getSubjectByPlatformId("99999"))
            .thenReturn(Mono.empty());
        when(extensionComponentsFinder.getExtensions(MetaInfoExtensionPoint.class))
            .thenReturn(List.of(bgmExtension));

        StepVerifier.create(
                service.getSubjectByPlatformId(SubjectSyncPlatform.BGM_TV, "99999"))
            .verifyComplete();
    }

    @Test
    void getSubjectByPlatformId_nullPlatform_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.getSubjectByPlatformId(null, "12345"));
    }

    @Test
    void getSubjectByPlatformId_blankPlatformId_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> service.getSubjectByPlatformId(SubjectSyncPlatform.BGM_TV, ""));
    }
}
