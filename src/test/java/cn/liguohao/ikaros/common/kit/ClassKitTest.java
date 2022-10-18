package cn.liguohao.ikaros.common.kit;

import cn.liguohao.ikaros.model.option.AppOptionModel;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author guohao
 * @date 2022/10/19
 */
class ClassKitTest {


    @Test
    void findClassByPackage() throws IOException {
        String packageName = AppOptionModel.class.getPackageName();

        List<Class<?>> classes = ClassKit.findClassByPackage(packageName);

        Assertions.assertNotNull(classes);
        Assertions.assertFalse(classes.isEmpty());
    }
}