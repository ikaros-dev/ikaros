package run.ikaros.server.core.binding.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.core.binding.DirectoryBindingContext;
import run.ikaros.api.core.binding.DirectoryBindingStep;
import run.ikaros.api.core.binding.DirectoryBindingStepStatus;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.core.binding.chain.DirectoryBindingChain;
import run.ikaros.server.core.binding.service.DirectoryBindingService;
import run.ikaros.server.core.task.Task;
import run.ikaros.server.core.task.TaskService;
import run.ikaros.server.plugin.ExtensionComponentsFinder;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.AttachmentRepository;
import run.ikaros.server.store.repository.DirectoryBindingWorkflowRepository;
import run.ikaros.server.store.repository.TaskRepository;

@Slf4j
@Service
public class DirectoryBindingServiceImpl implements DirectoryBindingService {

    private final TaskService taskService;
    private final TaskRepository taskRepository;
    private final DirectoryBindingWorkflowRepository workflowRepository;
    private final AttachmentRepository attachmentRepository;
    private final List<DirectoryBindingStep> builtInSteps;
    private final ExtensionComponentsFinder extensionComponentsFinder;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DirectoryBindingServiceImpl(TaskService taskService,
                                       TaskRepository taskRepository,
                                       DirectoryBindingWorkflowRepository workflowRepository,
                                       AttachmentRepository attachmentRepository,
                                       List<DirectoryBindingStep> builtInSteps,
                                       ExtensionComponentsFinder extensionComponentsFinder) {
        this.taskService = taskService;
        this.taskRepository = taskRepository;
        this.workflowRepository = workflowRepository;
        this.attachmentRepository = attachmentRepository;
        this.builtInSteps = builtInSteps;
        this.extensionComponentsFinder = extensionComponentsFinder;
    }

    @Override
    public Mono<DirectoryBindingWorkflowEntity> bindDirectory(UUID directoryId,
                                                              SubjectSyncPlatform platform,
                                                              String keyword) {
        return attachmentRepository.findById(directoryId)
            .switchIfEmpty(Mono.error(new IllegalArgumentException(
                "Directory not found for id: " + directoryId)))
            .flatMap(dirEntity -> {
                String dirName = dirEntity.getName();

                DirectoryBindingWorkflowEntity workflow = DirectoryBindingWorkflowEntity.builder()
                    .id(UUID.randomUUID())
                    .directoryId(directoryId)
                    .directoryName(dirName)
                    .platform(platform)
                    .status(TaskStatus.CREATE)
                    .createTime(LocalDateTime.now())
                    .build();

                return workflowRepository.insert(workflow)
                    .flatMap(savedWorkflow -> {
                        TaskEntity taskEntity = TaskEntity.builder()
                            .id(UUID.randomUUID())
                            .name("DirectoryBinding:" + dirName)
                            .status(TaskStatus.CREATE)
                            .createTime(LocalDateTime.now())
                            .build();

                        savedWorkflow.setTaskId(taskEntity.getId());

                        DirectoryBindingTask task = new DirectoryBindingTask(
                            taskEntity, taskRepository, savedWorkflow, workflowRepository,
                            dirName, directoryId, platform, keyword, buildAllSteps());

                        return taskService.submit(task)
                            .then(workflowRepository.update(savedWorkflow));
                    });
            });
    }

    @Override
    public Mono<Void> bindDirectories(UUID parentDirectoryId, SubjectSyncPlatform platform) {
        return attachmentRepository.findAllByParentId(parentDirectoryId)
            .filter(att -> att.getType() == AttachmentType.Directory
                || att.getType() == AttachmentType.Driver_Directory)
            .flatMap(dir -> bindDirectory(dir.getId(), platform))
            .then();
    }

    @Override
    public Mono<DirectoryBindingWorkflowEntity> findWorkflowById(UUID workflowId) {
        return workflowRepository.findById(workflowId);
    }

    @Override
    public Mono<DirectoryBindingWorkflowEntity> findWorkflowByTaskId(UUID taskId) {
        return workflowRepository.findByTaskId(taskId);
    }

    private List<DirectoryBindingStep> buildAllSteps() {
        List<DirectoryBindingStep> allSteps = new ArrayList<>(builtInSteps);

        // Merge plugin steps
        extensionComponentsFinder
            .getExtensions(run.ikaros.api.core.binding.DirectoryBindingChainPluginHook.class)
            .forEach(hook -> allSteps.addAll(hook.getAdditionalSteps()));

        return allSteps;
    }

    /**
     * Internal task that runs the binding chain.
     */
    private static class DirectoryBindingTask extends Task {
        private final DirectoryBindingWorkflowEntity workflow;
        private final DirectoryBindingWorkflowRepository workflowRepository;
        private final String directoryName;
        private final UUID directoryId;
        private final SubjectSyncPlatform platform;
        private final String keyword;
        private final List<DirectoryBindingStep> steps;

        DirectoryBindingTask(TaskEntity entity, TaskRepository repository,
                             DirectoryBindingWorkflowEntity workflow,
                             DirectoryBindingWorkflowRepository workflowRepository,
                             String directoryName, UUID directoryId,
                             SubjectSyncPlatform platform, String keyword,
                             List<DirectoryBindingStep> steps) {
            super(entity, repository);
            this.workflow = workflow;
            this.workflowRepository = workflowRepository;
            this.directoryName = directoryName;
            this.directoryId = directoryId;
            this.platform = platform;
            this.keyword = keyword;
            this.steps = steps;
        }

        @Override
        protected String getTaskEntityName() {
            return "DirectoryBinding:" + directoryName;
        }

        @Override
        protected void doRun() throws Exception {
            workflow.setStatus(TaskStatus.RUNNING);
            workflowRepository.update(workflow).block(AppConst.BLOCK_TIMEOUT);

            DirectoryBindingContext context =
                DirectoryBindingContext.create(directoryId, directoryName, platform);
            if (keyword != null && !keyword.isBlank()) {
                context.setKeyword(keyword);
            }

            getEntity().setTotal((long) steps.size());

            DirectoryBindingChain chain = new DirectoryBindingChain(steps);

            try {
                chain.execute(context).block(Duration.ofSeconds(240L));

                long completedCount = context.getStepResults().values().stream()
                    .filter(s -> s == DirectoryBindingStepStatus.SUCCESS)
                    .count();
                getEntity().setIndex(completedCount);

                if (context.getSubjectId() != null) {
                    workflow.setSubjectId(context.getSubjectId());
                }

                workflow.setStatus(TaskStatus.FINISH);
                workflow.setEndTime(LocalDateTime.now());
            } catch (Exception e) {
                workflow.setStatus(TaskStatus.FAIL);
                workflow.setEndTime(LocalDateTime.now());
                workflow.setFailMessage(e.getMessage());
                throw e;
            } finally {
                workflowRepository.update(workflow).block(AppConst.BLOCK_TIMEOUT);
            }
        }
    }
}
