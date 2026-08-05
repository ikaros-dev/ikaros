package run.ikaros.server.store.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.api.store.enums.SubjectSyncPlatform;
import run.ikaros.api.store.enums.TaskStatus;
import run.ikaros.server.config.IkarosTestcontainersConfiguration;
import run.ikaros.server.store.entity.DirectoryBindingWorkflowEntity;

/** 本地目录绑定工作流仓储测试。 */
@SpringBootTest
@Testcontainers
@Import(IkarosTestcontainersConfiguration.class)
class DirectoryBindingWorkflowRepositoryTest {
    @Autowired
    private DirectoryBindingWorkflowRepository repository;

    @AfterEach
    void tearDown() {
        StepVerifier.create(repository.deleteAll()).verifyComplete();
    }

    @Test
    void localWorkflowShouldSupportCrudAndExcludeRemoteRecord() {
        UUID directoryId = UuidV7Utils.generateUuid();
        UUID subjectId = UuidV7Utils.generateUuid();
        DirectoryBindingWorkflowEntity remote = DirectoryBindingWorkflowEntity.builder()
            .id(UuidV7Utils.generateUuid()).directoryId(directoryId).directoryName("remote")
            .subjectId(subjectId).platform(SubjectSyncPlatform.BGM_TV).localMode("EPISODE")
            .status(TaskStatus.CREATE).build();
        StepVerifier.create(repository.insert(remote)).expectNext(remote).verifyComplete();
        StepVerifier.create(repository.findLocalWorkflow(directoryId, subjectId, "EPISODE"))
            .verifyComplete();
        StepVerifier.create(repository.deleteById(remote.getId())).verifyComplete();

        DirectoryBindingWorkflowEntity entity = DirectoryBindingWorkflowEntity.builder()
            .id(UuidV7Utils.generateUuid()).directoryId(directoryId).directoryName("local")
            .subjectId(subjectId).localMode("EPISODE").status(TaskStatus.CREATE)
            .localScanState("{\"items\":[]}").build();

        StepVerifier.create(repository.insert(entity)).expectNext(entity).verifyComplete();

        DirectoryBindingWorkflowEntity found = repository.findLocalWorkflow(directoryId, subjectId,
            "EPISODE").block();
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(entity.getId());
        assertThat(found.getPlatform()).isNull();
        assertThat(found.getLocalScanState()).isEqualTo("{\"items\":[]}");
        assertThat(found.getVersion()).isZero();

        entity.setCurrentStep("已确认");
        StepVerifier.create(repository.update(entity))
            .assertNext(updated -> {
                assertThat(updated.getCurrentStep()).isEqualTo("已确认");
                assertThat(updated.getVersion()).isOne();
            })
            .verifyComplete();
        StepVerifier.create(repository.findById(entity.getId()))
            .assertNext(updated -> assertThat(updated.getCurrentStep()).isEqualTo("已确认"))
            .verifyComplete();
        StepVerifier.create(repository.deleteById(entity.getId())).verifyComplete();
        StepVerifier.create(repository.existsById(entity.getId()))
            .expectNext(false).verifyComplete();
    }
}
