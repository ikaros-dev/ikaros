package cn.liguohao.ikaros.common;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
class SystemVarKitTest {

    @Test
    void getCurrentAppDir() {
        String currentAppDir = SystemVarKit.getCurrentAppDirPath();
        Assertions.assertNotNull(currentAppDir);
        Assertions.assertTrue(currentAppDir.contains("ikaros"));
    }

    @Test
    void getCurrentUserName() {
        String currentUserName = SystemVarKit.getCurrentUserName();
        Assertions.assertNotNull(currentUserName);
    }

    @Test
    void getCurrentUserDir() {
        String currentUserDir = SystemVarKit.getCurrentUserDirPath();
        Assertions.assertNotNull(currentUserDir);
        Assertions.assertTrue(currentUserDir.contains(SystemVarKit.getCurrentUserName()));
    }
}