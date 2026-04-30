package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RandomUtilsTest {

    @Test
    void randomStringShouldReturnCorrectLength() {
        assertThat(RandomUtils.randomString(10)).hasSize(10);
        assertThat(RandomUtils.randomString(1)).hasSize(1);
        assertThat(RandomUtils.randomString(100)).hasSize(100);
    }

    @Test
    void randomStringShouldReturnDigitsOnly() {
        String result = RandomUtils.randomString(50);
        assertThat(result).matches("\\d+");
    }

    @Test
    void randomStringShouldHandleNonPositiveLength() {
        // When length <= 0, defaults to 10
        assertThat(RandomUtils.randomString(0)).hasSize(10);
        assertThat(RandomUtils.randomString(-1)).hasSize(10);
    }

    @Test
    void randomStringShouldReturnUniqueValues() {
        String s1 = RandomUtils.randomString(20);
        String s2 = RandomUtils.randomString(20);
        assertThat(s1).isNotEqualTo(s2);
    }

    @Test
    void getRandomShouldReturnNonNull() {
        assertThat(RandomUtils.getRandom()).isNotNull();
    }
}
