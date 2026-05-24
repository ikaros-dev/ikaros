package run.ikaros.api.constant;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AppConstTest {

    @Test
    void LOGIN_SUCCESS_LOCATION_ShouldBeCorrect() {
        assertEquals("/console/", AppConst.LOGIN_SUCCESS_LOCATION);
    }

    @Test
    void LOGIN_FAILURE_LOCATION_ShouldBeCorrect() {
        assertEquals("/console/?error#/login", AppConst.LOGIN_FAILURE_LOCATION);
    }

    @Test
    void LOGOUT_SUCCESS_LOCATION_ShouldBeCorrect() {
        assertEquals("/console/?logout", AppConst.LOGOUT_SUCCESS_LOCATION);
    }

    @Test
    void BLOCK_TIMEOUT_ShouldBeTwoSeconds() {
        assertEquals(Duration.ofMillis(2000L), AppConst.BLOCK_TIMEOUT);
        assertEquals(2000L, AppConst.BLOCK_TIMEOUT.toMillis());
    }

    @Test
    void CACHE_DIR_NAME_ShouldBeCorrect() {
        assertEquals("caches", AppConst.CACHE_DIR_NAME);
    }

    @Test
    void STATIC_DIR_NAME_ShouldBeCorrect() {
        assertEquals("statics", AppConst.STATIC_DIR_NAME);
    }

    @Test
    void STATIC_FONT_DIR_NAME_ShouldBeCorrect() {
        assertEquals("fonts", AppConst.STATIC_FONT_DIR_NAME);
    }

    @Test
    void THEME_DIR_NAME_ShouldBeCorrect() {
        assertEquals("themes", AppConst.THEME_DIR_NAME);
    }

    @Test
    void EPISODE_FINISH_ShouldBeCorrectValue() {
        double expected = 0.9375 * 0.85;
        assertEquals(expected, AppConst.EPISODE_FINISH, 0.0001);
    }

    @Test
    void EPISODE_FINISH_ShouldBeLessThanOne() {
        assertTrue(AppConst.EPISODE_FINISH < 1.0);
    }

    @Test
    void EPISODE_FINISH_ShouldBeGreaterThanZero() {
        assertTrue(AppConst.EPISODE_FINISH > 0.0);
    }
}