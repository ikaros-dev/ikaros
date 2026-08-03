package run.ikaros.server.core.binding.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.LocalMediaMode;
import run.ikaros.api.core.binding.LocalScanConfirmRequest;
import run.ikaros.api.core.binding.LocalScanPreview;
import run.ikaros.api.core.binding.LocalScanPreviewRequest;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.core.binding.LocalDirectoryBindingService;
import run.ikaros.server.core.binding.service.DirectoryBindingService;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;

/** 验证目录绑定接口的路由、参数委托和响应状态。 */
class DirectoryBindingEndpointTest {

    /** 远程目录绑定服务。 */
    @Mock
    private DirectoryBindingService service;
    /** 本地目录绑定服务。 */
    @Mock
    private LocalDirectoryBindingService localService;
    /** 用于调用函数式路由的测试客户端。 */
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        DirectoryBindingEndpoint endpoint = new DirectoryBindingEndpoint(service, localService);
        webTestClient = WebTestClient.bindToRouterFunction(endpoint.endpoint()).build();
    }

    @Test
    void endpoint_returnsRouterFunction() {
        assertThat(webTestClient).isNotNull();
    }

    @Test
    void bindDirectory_delegatesToService() {
        UUID directoryId = UUID.randomUUID();
        when(service.bindDirectory(directoryId, SubjectSyncPlatform.BGM_TV, null, null))
            .thenReturn(Mono.just(DirectoryBindingWorkflowEntity.builder()
                .id(UUID.randomUUID()).directoryId(directoryId)
                .directoryName("Test Anime").platform(SubjectSyncPlatform.BGM_TV)
                .status(TaskStatus.CREATE).createTime(LocalDateTime.now()).build()));

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder.path("/binding/directory")
                .queryParam("directoryId", directoryId)
                .queryParam("platform", SubjectSyncPlatform.BGM_TV)
                .build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.directoryId").isEqualTo(directoryId.toString());

        verify(service).bindDirectory(directoryId, SubjectSyncPlatform.BGM_TV, null, null);
    }

    @Test
    void bindDirectory_withKeyword_delegatesToService() {
        UUID directoryId = UUID.randomUUID();
        String keyword = "Custom Keyword";
        when(service.bindDirectory(directoryId, SubjectSyncPlatform.BGM_TV, keyword, null))
            .thenReturn(Mono.just(DirectoryBindingWorkflowEntity.builder()
                .id(UUID.randomUUID()).directoryId(directoryId)
                .directoryName("Test Anime").platform(SubjectSyncPlatform.BGM_TV)
                .status(TaskStatus.CREATE).build()));

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder.path("/binding/directory")
                .queryParam("directoryId", directoryId)
                .queryParam("platform", SubjectSyncPlatform.BGM_TV)
                .queryParam("keyword", keyword)
                .build())
            .exchange()
            .expectStatus().isOk();

        verify(service).bindDirectory(directoryId, SubjectSyncPlatform.BGM_TV, keyword, null);
    }

    @Test
    void bindDirectories_delegatesToService() {
        UUID parentDirectoryId = UUID.randomUUID();
        when(service.bindDirectories(parentDirectoryId, SubjectSyncPlatform.BGM_TV))
            .thenReturn(Mono.empty());

        webTestClient.post()
            .uri(uriBuilder -> uriBuilder.path("/binding/directories")
                .queryParam("parentDirectoryId", parentDirectoryId)
                .queryParam("platform", SubjectSyncPlatform.BGM_TV)
                .build())
            .exchange()
            .expectStatus().isOk();

        verify(service).bindDirectories(parentDirectoryId, SubjectSyncPlatform.BGM_TV);
    }

    @Test
    void findWorkflowById_delegatesToService() {
        UUID workflowId = UUID.randomUUID();
        when(service.findWorkflowById(workflowId))
            .thenReturn(Mono.just(DirectoryBindingWorkflowEntity.builder()
                .id(workflowId).status(TaskStatus.FINISH).build()));

        webTestClient.get().uri("/binding/workflow/{id}", workflowId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(workflowId.toString());

        verify(service).findWorkflowById(workflowId);
    }

    @Test
    void findWorkflowByTaskId_delegatesToService() {
        UUID taskId = UUID.randomUUID();
        when(service.findWorkflowByTaskId(taskId))
            .thenReturn(Mono.just(DirectoryBindingWorkflowEntity.builder()
                .id(UUID.randomUUID()).taskId(taskId).status(TaskStatus.RUNNING).build()));

        webTestClient.get().uri("/binding/workflow/task/{taskId}", taskId)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.taskId").isEqualTo(taskId.toString());

        verify(service).findWorkflowByTaskId(taskId);
    }

    @Test
    void previewLocalDirectory_returnsPreview() {
        UUID directoryId = UUID.randomUUID();
        LocalScanPreview preview = LocalScanPreview.builder()
            .directoryId(directoryId).mode(LocalMediaMode.EPISODE).items(List.of()).build();
        when(localService.preview(any(LocalScanPreviewRequest.class)))
            .thenReturn(Mono.just(preview));

        webTestClient.post().uri("/binding/local/preview")
            .bodyValue(LocalScanPreviewRequest.builder()
                .directoryId(directoryId).mode(LocalMediaMode.EPISODE).build())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.directory_id").isEqualTo(directoryId.toString())
            .jsonPath("$.mode").isEqualTo(LocalMediaMode.EPISODE.name());

        verify(localService).preview(any(LocalScanPreviewRequest.class));
    }

    @Test
    void previewLocalDirectory_returnsNotFoundForMissingDirectory() {
        when(localService.preview(any(LocalScanPreviewRequest.class)))
            .thenReturn(Mono.error(new IllegalArgumentException("待扫描目录附件不存在")));

        webTestClient.post().uri("/binding/local/preview")
            .bodyValue(LocalScanPreviewRequest.builder()
                .directoryId(UUID.randomUUID()).mode(LocalMediaMode.EPISODE).build())
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(String.class).isEqualTo("待扫描目录附件不存在");
    }

    @Test
    void confirmLocalDirectory_returnsWorkflowAndAllowsIdempotentConfirmation() {
        UUID directoryId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        DirectoryBindingWorkflowEntity workflow = localWorkflow(directoryId, subjectId);
        when(localService.confirm(any(LocalScanConfirmRequest.class)))
            .thenReturn(Mono.just(workflow));
        LocalScanConfirmRequest request = LocalScanConfirmRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.EPISODE).subjectId(subjectId).build();

        webTestClient.post().uri("/binding/local/confirm").bodyValue(request)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo(workflow.getId().toString());
        webTestClient.post().uri("/binding/local/confirm").bodyValue(request)
            .exchange()
            .expectStatus().isOk();

        verify(localService, times(2)).confirm(any(LocalScanConfirmRequest.class));
    }

    @Test
    void confirmLocalDirectory_returnsBadRequestForInvalidConfirmation() {
        when(localService.confirm(any(LocalScanConfirmRequest.class)))
            .thenReturn(Mono.error(new IllegalArgumentException(
                "必须恰好提供一个条目，且目录和扫描模式不能为空")));

        webTestClient.post().uri("/binding/local/confirm")
            .bodyValue(LocalScanConfirmRequest.builder()
                .directoryId(UUID.randomUUID()).mode(LocalMediaMode.EPISODE).build())
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(String.class)
            .isEqualTo("必须恰好提供一个条目，且目录和扫描模式不能为空");
    }

    @Test
    void confirmLocalDirectory_returnsBadRequestForEmptyPrimaryItems() {
        when(localService.confirm(any(LocalScanConfirmRequest.class)))
            .thenReturn(Mono.error(new IllegalArgumentException("扫描结果中没有可确认的主资源")));

        webTestClient.post().uri("/binding/local/confirm")
            .bodyValue(LocalScanConfirmRequest.builder()
                .directoryId(UUID.randomUUID()).mode(LocalMediaMode.EPISODE)
                .subjectId(UUID.randomUUID()).build())
            .exchange()
            .expectStatus().isBadRequest()
            .expectBody(String.class).isEqualTo("扫描结果中没有可确认的主资源");
    }

    @Test
    void confirmLocalDirectory_returnsConflictForConcurrentConfirmation() {
        when(localService.confirm(any(LocalScanConfirmRequest.class)))
            .thenReturn(Mono.error(new OptimisticLockingFailureException("并发更新")));

        webTestClient.post().uri("/binding/local/confirm")
            .bodyValue(LocalScanConfirmRequest.builder()
                .directoryId(UUID.randomUUID()).mode(LocalMediaMode.EPISODE)
                .subjectId(UUID.randomUUID()).build())
            .exchange()
            .expectStatus().isEqualTo(409)
            .expectBody(String.class).isEqualTo("本地绑定正在被并发处理");
    }

    @Test
    void rescanLocalDirectory_returnsSubmittedWorkflow() {
        UUID directoryId = UUID.randomUUID();
        UUID subjectId = UUID.randomUUID();
        DirectoryBindingWorkflowEntity workflow = localWorkflow(directoryId, subjectId);
        when(service.findWorkflowById(workflow.getId())).thenReturn(Mono.just(workflow));
        when(localService.rescan(directoryId, subjectId, LocalMediaMode.EPISODE))
            .thenReturn(Mono.just(workflow));

        webTestClient.post().uri("/binding/local/workflow/{id}/rescan", workflow.getId())
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.taskId").isEqualTo(workflow.getTaskId().toString());

        verify(localService).rescan(directoryId, subjectId, LocalMediaMode.EPISODE);
    }

    @Test
    void rescanLocalDirectory_returnsNotFoundForMissingWorkflow() {
        UUID workflowId = UUID.randomUUID();
        when(service.findWorkflowById(workflowId)).thenReturn(Mono.empty());

        webTestClient.post().uri("/binding/local/workflow/{id}/rescan", workflowId)
            .exchange()
            .expectStatus().isNotFound()
            .expectBody(String.class).isEqualTo("未找到本地目录绑定工作流");

        verify(localService, never()).rescan(any(), any(), any());
    }

    private DirectoryBindingWorkflowEntity localWorkflow(UUID directoryId, UUID subjectId) {
        return DirectoryBindingWorkflowEntity.builder()
            .id(UUID.randomUUID())
            .taskId(UUID.randomUUID())
            .directoryId(directoryId)
            .directoryName("本地目录")
            .subjectId(subjectId)
            .localMode(LocalMediaMode.EPISODE.name())
            .status(TaskStatus.CREATE)
            .createTime(LocalDateTime.now())
            .build();
    }
}
