package run.ikaros.server;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import run.ikaros.server.plugin.listener.PluginPropertiesEnablesInitListener;
import run.ikaros.server.security.MasterInitializer;

/**
 * 负责按顺序执行启动初始化任务并输出启动摘要.
 */
@Component
public class StartupInitializer {

    /** 默认管理员初始化器. */
    private final MasterInitializer masterInitializer;

    /** 插件初始化器. */
    private final PluginPropertiesEnablesInitListener pluginInitializer;

    /** 启动摘要日志输出器. */
    private final StartupSummaryLogger startupSummaryLogger;

    /**
     * 创建启动初始化协调器.
     *
     * @param masterInitializer 默认管理员初始化器
     * @param pluginInitializer 插件初始化器
     * @param startupSummaryLogger 启动摘要日志输出器
     */
    public StartupInitializer(MasterInitializer masterInitializer,
                              PluginPropertiesEnablesInitListener pluginInitializer,
                              StartupSummaryLogger startupSummaryLogger) {
        this.masterInitializer = masterInitializer;
        this.pluginInitializer = pluginInitializer;
        this.startupSummaryLogger = startupSummaryLogger;
    }

    /**
     * 在应用就绪后依次初始化账户和插件，最后输出启动摘要.
     *
     * @return 初始化完成信号
     */
    @EventListener(ApplicationReadyEvent.class)
    public Mono<Void> initialize() {
        return masterInitializer.initialize()
            .then(pluginInitializer.initialize())
            .then(Mono.fromRunnable(startupSummaryLogger::log));
    }
}
