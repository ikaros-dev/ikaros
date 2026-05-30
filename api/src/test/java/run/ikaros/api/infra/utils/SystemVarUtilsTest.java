package run.ikaros.api.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import org.junit.jupiter.api.Test;

class SystemVarUtilsTest {

    @Test
    void getCurrentAppDirPath_ShouldReturnNonNullPath() {
        String path = SystemVarUtils.getCurrentAppDirPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());
    }

    @Test
    void getCurrentAppDirPath_ShouldReturnValidDirectory() {
        String path = SystemVarUtils.getCurrentAppDirPath();
        File dir = new File(path);
        assertTrue(dir.exists() || path.contains("ikaros"));
    }

    @Test
    void getCurrentAppOriginalDirPath_ShouldReturnNonNullPath() {
        String path = SystemVarUtils.getCurrentAppOriginalDirPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());
    }

    @Test
    void getCurrentAppOriginalDirPath_ShouldContainOriginalDirName() {
        String path = SystemVarUtils.getCurrentAppOriginalDirPath();
        assertTrue(path.contains("original") || path.contains("ORIGINAL"));
    }

    @Test
    void getCurrentAppOriginalDirPath_ShouldBeSubdirectoryOfAppDir() {
        String appDir = SystemVarUtils.getCurrentAppDirPath();
        String originalDir = SystemVarUtils.getCurrentAppOriginalDirPath();
        assertTrue(originalDir.startsWith(appDir));
    }

    @Test
    void getCurrentAppOriginalDirPath_ShouldEndWithOriginal() {
        String path = SystemVarUtils.getCurrentAppOriginalDirPath();
        assertTrue(path.endsWith("original"));
    }

    @Test
    void getCurrentAppDirPath_ShouldBeConsistent() {
        String path1 = SystemVarUtils.getCurrentAppDirPath();
        String path2 = SystemVarUtils.getCurrentAppDirPath();
        assertEquals(path1, path2);
    }

    @Test
    void getCurrentAppOriginalDirPath_ShouldBeConsistent() {
        String path1 = SystemVarUtils.getCurrentAppOriginalDirPath();
        String path2 = SystemVarUtils.getCurrentAppOriginalDirPath();
        assertEquals(path1, path2);
    }
}
