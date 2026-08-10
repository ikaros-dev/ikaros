package run.ikaros.server.core.subject.vo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import run.ikaros.api.core.subject.vo.FindSubjectCondition;
import run.ikaros.api.store.enums.SubjectType;

class FindSubjectConditionTest {

    @Test
    void getNsfw() {
        Boolean nsfw = null;
        FindSubjectCondition condition = FindSubjectCondition.builder().nsfw(nsfw).build();
        Assertions.assertThat(condition.getNsfw()).isNull();
    }

    @Test
    void initDefaultIfNullInitializesPageSizeAndTypes() {
        FindSubjectCondition condition = FindSubjectCondition.builder()
            .types(null)
            .build();

        condition.initDefaultIfNull();

        Assertions.assertThat(condition.getPage()).isEqualTo(1);
        Assertions.assertThat(condition.getSize()).isEqualTo(10);
        Assertions.assertThat(condition.getTypes()).isEmpty();
    }

    @Test
    void builderInitializesTypesAsEmptySet() {
        FindSubjectCondition condition = FindSubjectCondition.builder().build();

        Assertions.assertThat(condition.getTypes()).isEmpty();
        Assertions.assertThat(condition.getTypes()).doesNotContain(SubjectType.VIDEO);
    }
}
