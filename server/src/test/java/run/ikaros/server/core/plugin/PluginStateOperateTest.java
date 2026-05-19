package run.ikaros.server.core.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PluginStateOperateTest {

    @Test
    void startExists() {
        assertThat(PluginStateOperate.START).isNotNull();
        assertThat(PluginStateOperate.START.name()).isEqualTo("START");
    }

    @Test
    void stopExists() {
        assertThat(PluginStateOperate.STOP).isNotNull();
        assertThat(PluginStateOperate.STOP.name()).isEqualTo("STOP");
    }

    @Test
    void enableExists() {
        assertThat(PluginStateOperate.ENABLE).isNotNull();
        assertThat(PluginStateOperate.ENABLE.name()).isEqualTo("ENABLE");
    }

    @Test
    void disableExists() {
        assertThat(PluginStateOperate.DISABLE).isNotNull();
        assertThat(PluginStateOperate.DISABLE.name()).isEqualTo("DISABLE");
    }

    @Test
    void loadExists() {
        assertThat(PluginStateOperate.LOAD).isNotNull();
        assertThat(PluginStateOperate.LOAD.name()).isEqualTo("LOAD");
    }

    @Test
    void loadAllExists() {
        assertThat(PluginStateOperate.LOAD_ALL).isNotNull();
        assertThat(PluginStateOperate.LOAD_ALL.name()).isEqualTo("LOAD_ALL");
    }

    @Test
    void reloadExists() {
        assertThat(PluginStateOperate.RELOAD).isNotNull();
        assertThat(PluginStateOperate.RELOAD.name()).isEqualTo("RELOAD");
    }

    @Test
    void reloadAllExists() {
        assertThat(PluginStateOperate.RELOAD_ALL).isNotNull();
        assertThat(PluginStateOperate.RELOAD_ALL.name()).isEqualTo("RELOAD_ALL");
    }

    @Test
    void reloadAllStartedExists() {
        assertThat(PluginStateOperate.RELOAD_ALL_STARTED).isNotNull();
        assertThat(PluginStateOperate.RELOAD_ALL_STARTED.name()).isEqualTo("RELOAD_ALL_STARTED");
    }

    @Test
    void deleteExists() {
        assertThat(PluginStateOperate.DELETE).isNotNull();
        assertThat(PluginStateOperate.DELETE.name()).isEqualTo("DELETE");
    }

    @Test
    void unloadExists() {
        assertThat(PluginStateOperate.UNLOAD).isNotNull();
        assertThat(PluginStateOperate.UNLOAD.name()).isEqualTo("UNLOAD");
    }

    @Test
    void totalValuesCount() {
        assertThat(PluginStateOperate.values()).hasSize(11);
    }

    @Test
    void valueOfReturnsCorrectEnum() {
        assertThat(PluginStateOperate.valueOf("START")).isEqualTo(PluginStateOperate.START);
        assertThat(PluginStateOperate.valueOf("STOP")).isEqualTo(PluginStateOperate.STOP);
        assertThat(PluginStateOperate.valueOf("ENABLE")).isEqualTo(PluginStateOperate.ENABLE);
        assertThat(PluginStateOperate.valueOf("DISABLE")).isEqualTo(PluginStateOperate.DISABLE);
        assertThat(PluginStateOperate.valueOf("LOAD")).isEqualTo(PluginStateOperate.LOAD);
        assertThat(PluginStateOperate.valueOf("LOAD_ALL")).isEqualTo(PluginStateOperate.LOAD_ALL);
        assertThat(PluginStateOperate.valueOf("RELOAD")).isEqualTo(PluginStateOperate.RELOAD);
        assertThat(PluginStateOperate.valueOf("RELOAD_ALL"))
            .isEqualTo(PluginStateOperate.RELOAD_ALL);
        assertThat(PluginStateOperate.valueOf("RELOAD_ALL_STARTED"))
            .isEqualTo(PluginStateOperate.RELOAD_ALL_STARTED);
        assertThat(PluginStateOperate.valueOf("DELETE")).isEqualTo(PluginStateOperate.DELETE);
        assertThat(PluginStateOperate.valueOf("UNLOAD")).isEqualTo(PluginStateOperate.UNLOAD);
    }
}
