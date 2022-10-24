package run.ikaros.server.utils;

import run.ikaros.server.model.option.AppOptionModel;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import run.ikaros.server.utils.ClassUtils;

/**
 * @author guohao
 * @date 2022/10/19
 */
class ClassUtilsTest {


    @Test
    void findClassByPackage() throws IOException {
        String packageName = AppOptionModel.class.getPackageName();

        List<Class<?>> classes = ClassUtils.findClassByPackage(packageName);

        Assertions.assertNotNull(classes);
        Assertions.assertFalse(classes.isEmpty());
    }
}