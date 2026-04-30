package run.ikaros.server.infra.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ThemeConstTest {

    @Test
    void simpleShouldNotBeNull() {
        assertThat(ThemeConst.SIMPLE).isNotNull();
    }

    @Test
    void simpleShouldHaveCorrectValue() {
        assertThat(ThemeConst.SIMPLE).isEqualTo("simple");
    }

    @Test
    void defaultShouldEqualSimple() {
        assertThat(ThemeConst.DEFAULT).isEqualTo(ThemeConst.SIMPLE);
    }

    @Test
    void defaultShouldHaveCorrectValue() {
        assertThat(ThemeConst.DEFAULT).isEqualTo("simple");
    }

    @Test
    void simpleShouldNotBeEmpty() {
        assertThat(ThemeConst.SIMPLE).isNotEmpty();
    }

    @Test
    void defaultShouldNotBeEmpty() {
        assertThat(ThemeConst.DEFAULT).isNotEmpty();
    }
}
