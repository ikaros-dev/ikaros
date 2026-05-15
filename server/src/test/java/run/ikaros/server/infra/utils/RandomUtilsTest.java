package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RandomUtilsTest {

    @Test
    void randomString_positiveLength() {
        String result = RandomUtils.randomString(5);
        assertNotNull(result);
        assertEquals(5, result.length());
    }

    @Test
    void randomString_containsOnlyDigits() {
        String result = RandomUtils.randomString(10);
        assertTrue(result.matches("\\d{10}"));
    }

    @Test
    void randomString_zeroLength_defaultsTo10() {
        String result = RandomUtils.randomString(0);
        assertEquals(10, result.length());
    }

    @Test
    void randomString_negativeLength_defaultsTo10() {
        String result = RandomUtils.randomString(-5);
        assertEquals(10, result.length());
    }

    @Test
    void randomString_largeLength() {
        String result = RandomUtils.randomString(100);
        assertEquals(100, result.length());
    }

    @Test
    void getRandom_returnsNonNull() {
        assertNotNull(RandomUtils.getRandom());
    }
}
