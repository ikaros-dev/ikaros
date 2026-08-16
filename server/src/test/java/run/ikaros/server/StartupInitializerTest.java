package run.ikaros.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.plugin.listener.PluginPropertiesEnablesInitListener;
import run.ikaros.server.security.MasterInitializer;

/**
 * 验证启动初始化任务的执行顺序.
 */
class StartupInitializerTest {

    @Test
    void initializeRunsTasksInOrder() {
        MasterInitializer masterInitializer = mock(MasterInitializer.class);
        PluginPropertiesEnablesInitListener pluginInitializer =
            mock(PluginPropertiesEnablesInitListener.class);
        StartupSummaryLogger startupSummaryLogger = mock(StartupSummaryLogger.class);
        List<String> calls = new ArrayList<>();

        when(masterInitializer.initialize())
            .thenReturn(Mono.fromRunnable(() -> calls.add("master")));
        when(pluginInitializer.initialize())
            .thenReturn(Mono.fromRunnable(() -> calls.add("plugin")));
        doAnswer(invocation -> {
            calls.add("summary");
            return null;
        }).when(startupSummaryLogger).log();

        StartupInitializer initializer = new StartupInitializer(
            masterInitializer, pluginInitializer, startupSummaryLogger);

        StepVerifier.create(initializer.initialize())
            .verifyComplete();

        assertThat(calls).containsExactly("master", "plugin", "summary");
    }
}
