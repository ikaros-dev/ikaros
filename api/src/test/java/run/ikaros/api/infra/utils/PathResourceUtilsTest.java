package run.ikaros.api.infra.utils;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class PathResourceUtilsTest {

    @Test
    void getClasses() throws Exception {
        final String packageName = "run.ikaros.api.store.enums";
        List<Class<?>> classes = PathResourceUtils.getClasses(packageName);
        Assertions.assertThat(classes.isEmpty()).isFalse();
    }
}