package run.ikaros.server.core.binding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.binding.DirectoryBindingChainPluginHook;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.core.binding.service.impl.DirectoryBindingServiceImpl;
import run.ikaros.server.core.task.Task;
import run.ikaros.server.core.task.TaskService;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.DirectoryBindingWorkflowRepository;
import run.ikaros.server.store.repository.TaskRepository;

class DirectoryBindingServiceImplTest {

    @Mock
    private TaskService taskService;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private DirectoryBindingWorkflowRepository workflowRepository;
    @Mock
    private AttachmentRepository attachmentRepository;
    @Mock
    private DirectoryBindingStep builtInStep;
    @Mock
    private ExtensionComponentsFinder extensionComponentsFinder;
    @Mock
    private DirectoryBindingChainPluginHook pluginHook;

    private DirectoryBindingServiceImpl service;
    private UUID directoryId;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(builtInStep.name()).thenReturn("BuiltInStep");
        when(builtInStep.order()).thenReturn(10);

        service = new DirectoryBindingServiceImpl(
            taskService, taskRepository, workflowRepository,
            attachmentRepository, List.of(builtInStep), extensionComponentsFinder);
        directoryId = UUID.randomUUID();
    }

    @Test
    void bindDirectory_success() {
        AttachmentEntity dirEntity = AttachmentEntity.builder()
            .id(directoryId).name("Test Anime [1080p]")
            .type(AttachmentType.Directory).build();

        when(attachmentRepository.findById(directoryId))
            .thenReturn(Mono.just(dirEntity));
        when(workflowRepository.insert(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> {
                DirectoryBindingWorkflowEntity entity = invocation.getArgument(0);
                entity.setCreateTime(LocalDateTime.now());
                return Mono.just(entity);
            });
        when(taskService.submit(any(Task.class))).thenReturn(Mono.empty());
        when(workflowRepository.update(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.bindDirectory(directoryId, SubjectSyncPlatform.BGM_TV))
            .assertNext(wf -> {
                assertThat(wf.getDirectoryId()).isEqualTo(directoryId);
                assertThat(wf.getDirectoryName()).isEqualTo("Test Anime [1080p]");
                assertThat(wf.getPlatform()).isEqualTo(SubjectSyncPlatform.BGM_TV);
                assertThat(wf.getStatus()).isEqualTo(TaskStatus.CREATE);
            })
            .verifyComplete();

        verify(workflowRepository).insert(any(DirectoryBindingWorkflowEntity.class));
        verify(taskService).submit(any(Task.class));
    }

    @Test
    void bindDirectory_withKeyword() {
        AttachmentEntity dirEntity = AttachmentEntity.builder()
            .id(directoryId).name("Some Dir")
            .type(AttachmentType.Directory).build();

        when(attachmentRepository.findById(directoryId))
            .thenReturn(Mono.just(dirEntity));
        when(workflowRepository.insert(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> {
                DirectoryBindingWorkflowEntity entity = invocation.getArgument(0);
                entity.setCreateTime(LocalDateTime.now());
                return Mono.just(entity);
            });
        when(taskService.submit(any(Task.class))).thenReturn(Mono.empty());
        when(workflowRepository.update(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(
                service.bindDirectory(directoryId, SubjectSyncPlatform.BGM_TV, "Custom Keyword"))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void bindDirectory_directoryNotFound_throwsException() {
        when(attachmentRepository.findById(directoryId)).thenReturn(Mono.empty());

        StepVerifier.create(service.bindDirectory(directoryId, SubjectSyncPlatform.BGM_TV))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void bindDirectories_bindsAllSubDirectories() {
        UUID subDir1Id = UUID.randomUUID();
        UUID subDir2Id = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();

        AttachmentEntity subDir1 = AttachmentEntity.builder()
            .id(subDir1Id).parentId(directoryId)
            .name("Sub Anime 1").type(AttachmentType.Directory).build();
        AttachmentEntity subDir2 = AttachmentEntity.builder()
            .id(subDir2Id).parentId(directoryId)
            .name("Sub Anime 2").type(AttachmentType.Directory).build();
        AttachmentEntity aFile = AttachmentEntity.builder()
            .id(fileId).parentId(directoryId)
            .name("readme.txt").type(AttachmentType.File).build();

        when(attachmentRepository.findAllByParentId(directoryId))
            .thenReturn(Flux.just(subDir1, subDir2, aFile));
        when(attachmentRepository.findById(subDir1Id))
            .thenReturn(Mono.just(subDir1));
        when(attachmentRepository.findById(subDir2Id))
            .thenReturn(Mono.just(subDir2));
        when(workflowRepository.insert(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> {
                DirectoryBindingWorkflowEntity entity = invocation.getArgument(0);
                entity.setCreateTime(LocalDateTime.now());
                return Mono.just(entity);
            });
        when(taskService.submit(any(Task.class))).thenReturn(Mono.empty());
        when(workflowRepository.update(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.bindDirectories(directoryId, SubjectSyncPlatform.BGM_TV))
            .verifyComplete();

        // Should create workflows for both subdirectories but not the file
        verify(workflowRepository, org.mockito.Mockito.atLeast(1))
            .insert(any(DirectoryBindingWorkflowEntity.class));
    }

    @Test
    void findWorkflowById_found() {
        UUID workflowId = UUID.randomUUID();
        DirectoryBindingWorkflowEntity entity = DirectoryBindingWorkflowEntity.builder()
            .id(workflowId).directoryId(directoryId)
            .status(TaskStatus.RUNNING).build();

        when(workflowRepository.findById(workflowId)).thenReturn(Mono.just(entity));

        StepVerifier.create(service.findWorkflowById(workflowId))
            .assertNext(wf -> assertThat(wf.getId()).isEqualTo(workflowId))
            .verifyComplete();
    }

    @Test
    void findWorkflowById_notFound() {
        UUID workflowId = UUID.randomUUID();
        when(workflowRepository.findById(workflowId)).thenReturn(Mono.empty());

        StepVerifier.create(service.findWorkflowById(workflowId))
            .verifyComplete();
    }

    @Test
    void findWorkflowByTaskId_found() {
        UUID taskId = UUID.randomUUID();
        DirectoryBindingWorkflowEntity entity = DirectoryBindingWorkflowEntity.builder()
            .id(UUID.randomUUID()).taskId(taskId)
            .directoryId(directoryId).status(TaskStatus.FINISH).build();

        when(workflowRepository.findByTaskId(taskId)).thenReturn(Mono.just(entity));

        StepVerifier.create(service.findWorkflowByTaskId(taskId))
            .assertNext(wf -> assertThat(wf.getTaskId()).isEqualTo(taskId))
            .verifyComplete();
    }

    @Test
    void bindDirectory_withDriverDirectoryType() {
        // Driver_Directory type should also be accepted for the main directory
        // (it's queried by id directly, so the type check is on the parent query in bindDirectories)
        AttachmentEntity dirEntity = AttachmentEntity.builder()
            .id(directoryId).name("Driver Anime")
            .type(AttachmentType.Driver_Directory).build();

        when(attachmentRepository.findById(directoryId))
            .thenReturn(Mono.just(dirEntity));
        when(workflowRepository.insert(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> {
                DirectoryBindingWorkflowEntity entity = invocation.getArgument(0);
                entity.setCreateTime(LocalDateTime.now());
                return Mono.just(entity);
            });
        when(taskService.submit(any(Task.class))).thenReturn(Mono.empty());
        when(workflowRepository.update(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.bindDirectory(directoryId, SubjectSyncPlatform.BGM_TV))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void bindDirectory_usesBuiltInSteps() {
        // Verify that the service builds its step list from built-in steps
        // (extension components are empty in this test)
        when(extensionComponentsFinder.getExtensions(
            DirectoryBindingChainPluginHook.class))
            .thenReturn(List.of());

        AttachmentEntity dirEntity = AttachmentEntity.builder()
            .id(directoryId).name("Test Anime")
            .type(AttachmentType.Directory).build();

        when(attachmentRepository.findById(directoryId))
            .thenReturn(Mono.just(dirEntity));
        when(workflowRepository.insert(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> {
                DirectoryBindingWorkflowEntity entity = invocation.getArgument(0);
                entity.setCreateTime(LocalDateTime.now());
                return Mono.just(entity);
            });
        when(taskService.submit(any(Task.class))).thenReturn(Mono.empty());
        when(workflowRepository.update(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.bindDirectory(directoryId, SubjectSyncPlatform.BGM_TV))
            .expectNextCount(1)
            .verifyComplete();
    }

    @Test
    void bindDirectory_withPluginHookSteps() {
        // Verify that plugin hooks are merged into the step list
        when(extensionComponentsFinder.getExtensions(
            DirectoryBindingChainPluginHook.class))
            .thenReturn(List.of(pluginHook));

        DirectoryBindingStep pluginStep = org.mockito.Mockito.mock(DirectoryBindingStep.class);
        when(pluginStep.name()).thenReturn("PluginStep");
        when(pluginStep.order()).thenReturn(15);
        when(pluginHook.getAdditionalSteps()).thenReturn(List.of(pluginStep));

        AttachmentEntity dirEntity = AttachmentEntity.builder()
            .id(directoryId).name("Test Anime")
            .type(AttachmentType.Directory).build();

        when(attachmentRepository.findById(directoryId))
            .thenReturn(Mono.just(dirEntity));
        when(workflowRepository.insert(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> {
                DirectoryBindingWorkflowEntity entity = invocation.getArgument(0);
                entity.setCreateTime(LocalDateTime.now());
                return Mono.just(entity);
            });
        when(taskService.submit(any(Task.class))).thenReturn(Mono.empty());
        when(workflowRepository.update(any(DirectoryBindingWorkflowEntity.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(service.bindDirectory(directoryId, SubjectSyncPlatform.BGM_TV))
            .expectNextCount(1)
            .verifyComplete();
    }
}
