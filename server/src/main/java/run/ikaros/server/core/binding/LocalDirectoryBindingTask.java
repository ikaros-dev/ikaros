package run.ikaros.server.core.binding;

import java.time.LocalDateTime;
import org.springframework.util.StringUtils;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.core.task.Task;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;
import run.ikaros.server.store.entity.TaskEntity;
import run.ikaros.server.store.repository.DirectoryBindingWorkflowRepository;
import run.ikaros.server.store.repository.TaskRepository;

/** 负责记录本地目录确认或重扫执行结果的后台任务。 */
public class LocalDirectoryBindingTask extends Task {
    /** 对应的本地绑定工作流。 */
    private final DirectoryBindingWorkflowEntity workflow;
    /** 工作流持久化仓储。 */
    private final DirectoryBindingWorkflowRepository workflowRepository;

    public LocalDirectoryBindingTask(TaskEntity entity, TaskRepository taskRepository,
                                     DirectoryBindingWorkflowEntity workflow,
                                     DirectoryBindingWorkflowRepository workflowRepository) {
        super(entity, taskRepository);
        this.workflow = workflow;
        this.workflowRepository = workflowRepository;
    }

    @Override
    protected String getTaskEntityName() {
        return "LocalDirectoryBinding:" + workflow.getDirectoryName();
    }

    @Override
    protected void doRun() throws Exception {
        try {
            workflow.setStatus(TaskStatus.RUNNING);
            workflowRepository.update(workflow).block(AppConst.BLOCK_TIMEOUT);
            workflow.setStatus(TaskStatus.FINISH)
                .setCurrentStep("本地扫描状态已保存")
                .setEndTime(LocalDateTime.now());
            workflowRepository.update(workflow).block(AppConst.BLOCK_TIMEOUT);
        } catch (Exception exception) {
            workflow.setStatus(TaskStatus.FAIL)
                .setEndTime(LocalDateTime.now())
                .setFailMessage(StringUtils.hasText(exception.getMessage())
                    ? exception.getMessage() : "本地目录绑定任务失败");
            workflowRepository.update(workflow).block(AppConst.BLOCK_TIMEOUT);
            throw exception;
        }
    }
}
