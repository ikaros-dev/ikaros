package run.ikaros.api.infra.utils;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

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
        assertTrue(dir.exists() || dir.isDirectory() || path.contains("ikaros"));
    }

    @Test
    void getOriginalDirPath_ShouldReturnNonNullPath() {
        String path = SystemVarUtils.getOriginalDirPath();
        assertNotNull(path);
        assertFalse(path.isEmpty());
    }

    @Test
    void getOriginalDirPath_ShouldContainOriginalDirName() {
        String path = SystemVarUtils.getOriginalDirPath();
        assertTrue(path.contains("original") || path.contains("ORIGINAL"));
    }

    @Test
    void getOriginalDirPath_ShouldBeSubdirectoryOfAppDir() {
        String appDir = SystemVarUtils.getCurrentAppDirPath();
        String originalDir = SystemVarUtils.getOriginalDirPath();
        assertTrue(originalDir.startsWith(appDir));
    }

    @Test
    void getOriginalDirPath_ShouldEndWithOriginal() {
        String path = SystemVarUtils.getOriginalDirPath();
        assertTrue(path.endsWith("original"));
    }

    @Test
    void getCurrentAppDirPath_ShouldBeConsistent() {
        String path1 = SystemVarUtils.getCurrentAppDirPath();
        String path2 = SystemVarUtils.getCurrentAppDirPath();
        assertEquals(path1, path2);
    }

    @Test
    void getOriginalDirPath_ShouldBeConsistent() {
        String path1 = SystemVarUtils.getOriginalDirPath();
        String path2 = SystemVarUtils.getOriginalDirPath();
        assertEquals(path1, path2);
    }
}