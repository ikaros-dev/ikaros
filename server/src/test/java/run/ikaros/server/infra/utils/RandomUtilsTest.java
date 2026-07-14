package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RandomUtilsTest {

    @Test
    void randomString_positiveLength_returnsString() {
        String result = RandomUtils.randomString(10);
        assertThat(result).hasSize(10);
    }

    @Test
    void randomString_zeroLength_defaultsToLength10() {
        String result = RandomUtils.randomString(0);
        assertThat(result).hasSize(10);
    }

    @Test
    void randomString_negativeLength_defaultsToLength10() {
        String result = RandomUtils.randomString(-1);
        assertThat(result).hasSize(10);
    }

    @Test
    void randomString_producesNumericString() {
        String result = RandomUtils.randomString(50);
        assertThat(result).hasSize(50);
        assertThat(result).matches("[0-9]+");
    }

    @Test
    void randomString_multipleCalls_producesDifferentResults() {
        String r1 = RandomUtils.randomString(20);
        String r2 = RandomUtils.randomString(20);
        // 理论上极小概率相同，但测试这个保证方法正常工作
        assertThat(r1).isNotEqualTo(r2);
    }
}
