package run.ikaros.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pf4j.PluginWrapper;
import org.springframework.core.env.Environment;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.server.plugin.IkarosPluginDescriptor;
import run.ikaros.server.plugin.IkarosPluginManager;
import run.ikaros.server.security.MasterInitializer;
import run.ikaros.server.security.SecurityProperties;

/**
 * 验证中英双语启动摘要的内容.
 */
class StartupSummaryLoggerTest {

    /** 应用运行环境. */
    private Environment environment;

    /** Ikaros 基础配置. */
    private IkarosProperties ikarosProperties;

    /** 安全配置. */
    private SecurityProperties securityProperties;

    /** 默认管理员初始化器. */
    private MasterInitializer masterInitializer;

    /** 插件管理器. */
    private IkarosPluginManager pluginManager;

    @BeforeEach
    void setUp() {
        environment = mock(Environment.class);
        ikarosProperties = new IkarosProperties();
        ikarosProperties.setExternalUrl(URI.create("http://localhost:9999"));
        securityProperties = new SecurityProperties();
        masterInitializer = mock(MasterInitializer.class);
        pluginManager = mock(IkarosPluginManager.class);

        when(environment.getProperty("server.port", Integer.class, 8080)).thenReturn(9999);
        when(environment.getProperty("local.server.port", Integer.class, 9999)).thenReturn(9999);
        when(pluginManager.getPlugins()).thenReturn(List.of());
    }

    @Test
    void buildSummaryIncludesCredentialsOnlyOnFirstStartup() {
        when(masterInitializer.getInitialPassword()).thenReturn(Optional.of("secret123"));

        String summary = createLogger().buildSummary();

        assertThat(summary)
            .contains("Ikaros 服务启动成功 / Ikaros service started successfully")
            .contains("服务地址 / Service address: http://localhost:9999")
            .contains("初始账号 / Initial account: tomoki")
            .contains("初始密码 / Initial password: secret123")
            .contains("初始密码仅在首次启动时显示")
            .contains("The initial password is shown only on first startup")
            .doesNotContain("已安装插件 / Installed plugins");
    }

    @Test
    void buildSummaryHidesCredentialsOnLaterStartup() {
        when(masterInitializer.getInitialPassword()).thenReturn(Optional.empty());

        String summary = createLogger().buildSummary();

        assertThat(summary)
            .contains("服务地址 / Service address: http://localhost:9999")
            .doesNotContain("初始账号 / Initial account")
            .doesNotContain("初始密码 / Initial password")
            .doesNotContain("password is shown only on first startup");
    }

    @Test
    void buildSummaryIncludesInstalledPlugins() {
        when(masterInitializer.getInitialPassword()).thenReturn(Optional.empty());
        PluginWrapper pluginWrapper = mock(PluginWrapper.class);
        IkarosPluginDescriptor descriptor = mock(IkarosPluginDescriptor.class);
        when(pluginWrapper.getPluginId()).thenReturn("plugin-bangumi");
        when(pluginWrapper.getDescriptor()).thenReturn(descriptor);
        when(descriptor.getDisplayName()).thenReturn("Bangumi");
        when(descriptor.getVersion()).thenReturn("1.2.0");
        when(pluginManager.getPlugins()).thenReturn(List.of(pluginWrapper));

        String summary = createLogger().buildSummary();

        assertThat(summary)
            .contains("已安装插件 / Installed plugins")
            .contains("• Bangumi (plugin-bangumi) [v1.2.0]");
    }

    private StartupSummaryLogger createLogger() {
        return new StartupSummaryLogger(environment, ikarosProperties, securityProperties,
            masterInitializer, pluginManager);
    }
}
