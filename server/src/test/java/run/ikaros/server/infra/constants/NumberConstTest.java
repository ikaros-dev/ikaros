package run.ikaros.server.infra.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NumberConstTest {

    @Test
    void unUseIdShouldNotBeNull() {
        assertThat(NumberConst.UN_USE_ID).isNotNull();
    }

    @Test
    void unUseIdShouldHaveCorrectValue() {
        assertThat(NumberConst.UN_USE_ID).isEqualTo(-1L);
    }

    @Test
    void unUseIdShouldBeNegative() {
        assertThat(NumberConst.UN_USE_ID).isNegative();
    }

    @Test
    void unUseIdShouldBeLongType() {
        assertThat(NumberConst.UN_USE_ID).isInstanceOf(Long.class);
    }

    @Test
    void unUseIdShouldBeMinusOne() {
        assertThat(NumberConst.UN_USE_ID.longValue()).isEqualTo(-1L);
    }
}
