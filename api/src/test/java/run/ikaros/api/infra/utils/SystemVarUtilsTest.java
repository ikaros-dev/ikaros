package run.ikaros.api.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import org.junit.jupiter.api.Test;

class SystemVarUtilsTest {

    @Test
    void getCurrentAppDirPathShouldReturnNonNull() {
        String path = SystemVarUtils.getCurrentAppDirPath();
        assertThat(path).isNotNull();
        assertThat(path).isNotEmpty();
    }

    @Test
    void getCurrentUserNameShouldReturnNonNull() {
        String userName = SystemVarUtils.getCurrentUserName();
        assertThat(userName).isNotNull();
        assertThat(userName).isNotEmpty();
    }

    @Test
    void getCurrentUserDirPathShouldReturnNonNull() {
        String homePath = SystemVarUtils.getCurrentUserDirPath();
        assertThat(homePath).isNotNull();
        assertThat(homePath).isNotEmpty();
    }

    @Test
    void getOsCacheDirPathWithNullBasePathShouldReturnTempDir() {
        String cachePath = SystemVarUtils.getOsCacheDirPath(null);
        assertThat(cachePath).isNotNull();
        assertThat(cachePath).contains("ikaros");
    }

    @Test
    void getOsCacheDirPathShouldEndWithIkaros() {
        String cachePath = SystemVarUtils.getOsCacheDirPath(null);
        assertThat(cachePath).endsWith("ikaros");
    }

    @Test
    void getIpAddressShouldReturnValidFormat() {
        String ip = SystemVarUtils.getIpAddress();
        // IP can be null if network is not available
        if (ip != null) {
            assertThat(ip).matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
        }
    }

    @Test
    void platformIsWindowsShouldReturnBoolean() {
        // On Windows CI, this should be true
        boolean result = SystemVarUtils.platformIsWindows();
        String osName = System.getProperty("os.name");
        if (osName != null && osName.contains("Windows")) {
            assertThat(result).isTrue();
        } else {
            assertThat(result).isFalse();
        }
    }

    @Test
    void getCurrentAppMediaDirPathShouldContainMedia() {
        String path = SystemVarUtils.getCurrentAppMediaDirPath();
        assertThat(path).contains("media");
        assertThat(path).endsWith(File.separator + "media");
    }

    @Test
    void getCurrentAppOriginalDirPathShouldContainOriginal() {
        String path = SystemVarUtils.getCurrentAppOriginalDirPath();
        assertThat(path).contains("original");
        assertThat(path).endsWith(File.separator + "original");
    }
}
