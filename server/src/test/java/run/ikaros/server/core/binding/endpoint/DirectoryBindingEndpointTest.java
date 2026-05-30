package run.ikaros.server.core.binding.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.core.binding.service.DirectoryBindingService;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;

/**
 * Unit test for {@link DirectoryBindingEndpoint} route definitions.
 */
class DirectoryBindingEndpointTest {

    @Mock
    private DirectoryBindingService service;
    private DirectoryBindingEndpoint endpoint;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        endpoint = new DirectoryBindingEndpoint(service);
    }

    @Test
    void endpoint_returnsRouterFunction() {
        RouterFunction<ServerResponse> routes = endpoint.endpoint();
        assertThat(routes).isNotNull();
    }

    @Test
    void bindDirectory_delegatesToService() {
        UUID dirId = UUID.randomUUID();
        when(service.bindDirectory(dirId, SubjectSyncPlatform.BGM_TV, null))
            .thenReturn(Mono.just(DirectoryBindingWorkflowEntity.builder()
                .id(UUID.randomUUID()).directoryId(dirId)
                .directoryName("Test Anime").platform(SubjectSyncPlatform.BGM_TV)
                .status(TaskStatus.CREATE).createTime(LocalDateTime.now()).build()));

        // Verify service is called correctly
        DirectoryBindingWorkflowEntity result =
            service.bindDirectory(dirId, SubjectSyncPlatform.BGM_TV, null).block();
        assertThat(result).isNotNull();
        assertThat(result.getDirectoryId()).isEqualTo(dirId);
    }

    @Test
    void bindDirectory_withKeyword_delegatesToService() {
        UUID dirId = UUID.randomUUID();
        String keyword = "Custom Keyword";
        when(service.bindDirectory(dirId, SubjectSyncPlatform.BGM_TV, keyword))
            .thenReturn(Mono.just(DirectoryBindingWorkflowEntity.builder()
                .id(UUID.randomUUID()).directoryId(dirId)
                .directoryName("Test Anime").platform(SubjectSyncPlatform.BGM_TV)
                .status(TaskStatus.CREATE).build()));

        DirectoryBindingWorkflowEntity result =
            service.bindDirectory(dirId, SubjectSyncPlatform.BGM_TV, keyword).block();
        assertThat(result).isNotNull();
    }

    @Test
    void bindDirectories_delegatesToService() {
        UUID parentDirId = UUID.randomUUID();
        when(service.bindDirectories(parentDirId, SubjectSyncPlatform.BGM_TV))
            .thenReturn(Mono.empty());

        service.bindDirectories(parentDirId, SubjectSyncPlatform.BGM_TV).block();
    }

    @Test
    void findWorkflowById_delegatesToService() {
        UUID workflowId = UUID.randomUUID();
        when(service.findWorkflowById(workflowId))
            .thenReturn(Mono.just(DirectoryBindingWorkflowEntity.builder()
                .id(workflowId).status(TaskStatus.FINISH).build()));

        DirectoryBindingWorkflowEntity result =
            service.findWorkflowById(workflowId).block();
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(workflowId);
    }

    @Test
    void findWorkflowByTaskId_delegatesToService() {
        UUID taskId = UUID.randomUUID();
        when(service.findWorkflowByTaskId(taskId))
            .thenReturn(Mono.just(DirectoryBindingWorkflowEntity.builder()
                .id(UUID.randomUUID()).taskId(taskId).status(TaskStatus.RUNNING).build()));

        DirectoryBindingWorkflowEntity result =
            service.findWorkflowByTaskId(taskId).block();
        assertThat(result).isNotNull();
        assertThat(result.getTaskId()).isEqualTo(taskId);
    }
}
