package run.ikaros.server.core.task;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import run.ikaros.api.store.enums.TaskStatus;

class FindTaskConditionTest {

    @Test
    void builder() {
        FindTaskCondition condition = FindTaskCondition.builder()
            .page(1)
            .size(10)
            .name("test")
            .status(TaskStatus.RUNNING)
            .build();

        assertThat(condition.getPage()).isEqualTo(1);
        assertThat(condition.getSize()).isEqualTo(10);
        assertThat(condition.getName()).isEqualTo("test");
        assertThat(condition.getStatus()).isEqualTo(TaskStatus.RUNNING);
    }

    @Test
    void builderWithNulls() {
        FindTaskCondition condition = FindTaskCondition.builder()
            .page(1)
            .size(10)
            .build();

        assertThat(condition.getPage()).isEqualTo(1);
        assertThat(condition.getSize()).isEqualTo(10);
        assertThat(condition.getName()).isNull();
        assertThat(condition.getStatus()).isNull();
    }

    @Test
    void setters() {
        FindTaskCondition condition = FindTaskCondition.builder()
            .page(2)
            .size(20)
            .name("name")
            .status(TaskStatus.FINISH)
            .build();

        assertThat(condition.getPage()).isEqualTo(2);
        assertThat(condition.getSize()).isEqualTo(20);
        assertThat(condition.getName()).isEqualTo("name");
        assertThat(condition.getStatus()).isEqualTo(TaskStatus.FINISH);
    }
}
