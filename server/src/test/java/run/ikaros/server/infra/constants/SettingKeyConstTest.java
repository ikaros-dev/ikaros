package run.ikaros.server.infra.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SettingKeyConstTest {

    @Test
    void themeSelectShouldNotBeNull() {
        assertThat(SettingKeyConst.THEME_SELECT).isNotNull();
    }

    @Test
    void themeSelectShouldHaveCorrectValue() {
        assertThat(SettingKeyConst.THEME_SELECT).isEqualTo("THEME_SELECT");
    }

    @Test
    void themeSelectShouldNotBeEmpty() {
        assertThat(SettingKeyConst.THEME_SELECT).isNotEmpty();
    }

    @Test
    void themeSelectShouldBeUpperCase() {
        assertThat(SettingKeyConst.THEME_SELECT).isEqualTo(
            SettingKeyConst.THEME_SELECT.toUpperCase());
    }
}
