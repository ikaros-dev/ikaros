package run.ikaros.server.utils;


import run.ikaros.server.utils.SystemVarUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
class SystemVarUtilsTest {

    @Test
    void getCurrentAppDir() {
        String currentAppDir = SystemVarUtils.getCurrentAppDirPath();
        Assertions.assertNotNull(currentAppDir);
        Assertions.assertTrue(currentAppDir.contains("ikaros"));
    }

    @Test
    void getCurrentUserName() {
        String currentUserName = SystemVarUtils.getCurrentUserName();
        Assertions.assertNotNull(currentUserName);
    }

    @Test
    void getCurrentUserDir() {
        String currentUserDir = SystemVarUtils.getCurrentUserDirPath();
        Assertions.assertNotNull(currentUserDir);
        Assertions.assertTrue(currentUserDir.contains(SystemVarUtils.getCurrentUserName()));
    }

    @Test
    void platformIsWindows() {
        boolean isWindows = SystemVarUtils.platformIsWindows();
        assertThat(isWindows).isTrue();
    }
}