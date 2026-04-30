package run.ikaros.api.infra.utils;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AssertUtilsTest {

    @Test
    void notNullShouldPassWhenObjectIsNotNull() {
        assertThatNoException()
            .isThrownBy(() -> AssertUtils.notNull("test", "param"));
    }

    @Test
    void notNullShouldThrowWhenObjectIsNull() {
        assertThatThrownBy(() -> AssertUtils.notNull(null, "param"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'param' must not be null");
    }

    @Test
    void notBlankShouldPassWhenStringHasText() {
        assertThatNoException()
            .isThrownBy(() -> AssertUtils.notBlank("test", "param"));
    }

    @Test
    void notBlankShouldThrowWhenStringIsNull() {
        assertThatThrownBy(() -> AssertUtils.notBlank(null, "param"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void notBlankShouldThrowWhenStringIsEmpty() {
        assertThatThrownBy(() -> AssertUtils.notBlank("", "param"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'param' must not be blank");
    }

    @Test
    void notBlankShouldThrowWhenStringIsBlank() {
        assertThatThrownBy(() -> AssertUtils.notBlank("   ", "param"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'param' must not be blank");
    }

    @Test
    void isPositiveShouldPassWhenNumberIsPositive() {
        assertThatNoException()
            .isThrownBy(() -> AssertUtils.isPositive(1, "param"));
    }

    @Test
    void isPositiveShouldPassWhenNumberIsZero() {
        assertThatNoException()
            .isThrownBy(() -> AssertUtils.isPositive(0, "param"));
    }

    @Test
    void isPositiveShouldThrowWhenNumberIsNegative() {
        assertThatThrownBy(() -> AssertUtils.isPositive(-1, "param"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'param' must be positive");
    }

    @Test
    void isTrueShouldPassWhenConditionIsTrue() {
        assertThatNoException()
            .isThrownBy(() -> AssertUtils.isTrue(true, "param"));
    }

    @Test
    void isTrueShouldThrowWhenConditionIsFalse() {
        assertThatThrownBy(() -> AssertUtils.isTrue(false, "param"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'param' must be true");
    }

    @Test
    void isFalseShouldPassWhenConditionIsFalse() {
        assertThatNoException()
            .isThrownBy(() -> AssertUtils.isFalse(false, "param"));
    }

    @Test
    void isFalseShouldThrowWhenConditionIsTrue() {
        assertThatThrownBy(() -> AssertUtils.isFalse(true, "param"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("'param' must be false");
    }
}
