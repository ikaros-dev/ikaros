package run.ikaros.server.core.subject.vo;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class FindSubjectConditionTest {

    @Test
    void getNsfw() {
        Boolean nsfw = null;
        FindSubjectCondition condition = FindSubjectCondition.builder().nsfw(nsfw).build();
        Assertions.assertThat(condition.getNsfw()).isNull();
    }
}