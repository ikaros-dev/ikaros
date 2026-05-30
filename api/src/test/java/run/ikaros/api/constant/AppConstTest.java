package run.ikaros.api.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class AppConstTest {

    @Test
    void loginSuccessLocation_ShouldBeCorrect() {
        assertEquals("/console/", AppConst.LOGIN_SUCCESS_LOCATION);
    }

    @Test
    void loginFailureLocation_ShouldBeCorrect() {
        assertEquals("/console/?error#/login", AppConst.LOGIN_FAILURE_LOCATION);
    }

    @Test
    void logoutSuccessLocation_ShouldBeCorrect() {
        assertEquals("/console/?logout", AppConst.LOGOUT_SUCCESS_LOCATION);
    }

    @Test
    void blockTimeout_ShouldBeTwoSeconds() {
        assertEquals(Duration.ofMillis(2000L), AppConst.BLOCK_TIMEOUT);
        assertEquals(2000L, AppConst.BLOCK_TIMEOUT.toMillis());
    }

    @Test
    void cacheDirName_ShouldBeCorrect() {
        assertEquals("caches", AppConst.CACHE_DIR_NAME);
    }

    @Test
    void staticDirName_ShouldBeCorrect() {
        assertEquals("statics", AppConst.STATIC_DIR_NAME);
    }

    @Test
    void staticFontDirName_ShouldBeCorrect() {
        assertEquals("fonts", AppConst.STATIC_FONT_DIR_NAME);
    }

    @Test
    void themeDirName_ShouldBeCorrect() {
        assertEquals("themes", AppConst.THEME_DIR_NAME);
    }

    @Test
    void episodeFinish_ShouldBeCorrectValue() {
        double expected = 0.9375 * 0.85;
        assertEquals(expected, AppConst.EPISODE_FINISH, 0.0001);
    }

    @Test
    void episodeFinish_ShouldBeLessThanOne() {
        assertTrue(AppConst.EPISODE_FINISH < 1.0);
    }

    @Test
    void episodeFinish_ShouldBeGreaterThanZero() {
        assertTrue(AppConst.EPISODE_FINISH > 0.0);
    }
}