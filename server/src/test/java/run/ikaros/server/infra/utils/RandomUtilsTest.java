package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RandomUtilsTest {

    @Test
    void randomStringFiveDigits() {
        String result = RandomUtils.randomString(5);
        assertEquals(5, result.length());
        assertTrue(result.matches("\\d+"), "Result should contain only digits");
    }

    @Test
    void randomStringZeroDefaultsToTenDigits() {
        String result = RandomUtils.randomString(0);
        assertEquals(10, result.length());
        assertTrue(result.matches("\\d+"), "Result should contain only digits");
    }

    @Test
    void randomStringNegativeDefaultsToTenDigits() {
        String result = RandomUtils.randomString(-1);
        assertEquals(10, result.length());
        assertTrue(result.matches("\\d+"), "Result should contain only digits");
    }

    @Test
    void randomStringOneDigit() {
        String result = RandomUtils.randomString(1);
        assertEquals(1, result.length());
        assertTrue(result.matches("\\d+"), "Result should contain only digits");
    }
}
