package run.ikaros.server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginWrapper;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.server.plugin.IkarosPluginDescriptor;
import run.ikaros.server.plugin.IkarosPluginManager;
import run.ikaros.server.security.MasterInitializer;
import run.ikaros.server.security.SecurityProperties;

/**
 * 负责生成并输出中英双语启动摘要.
 */
@Slf4j
@Component
public class StartupSummaryLogger {

    /** 启动文案资源包基础名称. */
    private static final String MESSAGE_BUNDLE_NAME = "i18n.startup-messages";

    /** 启动摘要展示的语言顺序. */
    private static final List<Locale> DISPLAY_LOCALES =
        List.of(Locale.SIMPLIFIED_CHINESE, Locale.ENGLISH);

    /** 应用运行环境. */
    private final Environment environment;

    /** Ikaros 基础配置. */
    private final IkarosProperties ikarosProperties;

    /** 安全配置. */
    private final SecurityProperties securityProperties;

    /** 默认管理员初始化器. */
    private final MasterInitializer masterInitializer;

    /** 插件管理器. */
    private final IkarosPluginManager pluginManager;

    /** 启动文案资源包. */
    private final List<ResourceBundle> messageBundles;

    /**
     * 创建启动摘要日志输出器.
     *
     * @param environment 应用运行环境
     * @param ikarosProperties Ikaros 基础配置
     * @param securityProperties 安全配置
     * @param masterInitializer 默认管理员初始化器
     * @param pluginManager 插件管理器
     */
    public StartupSummaryLogger(Environment environment,
                                IkarosProperties ikarosProperties,
                                SecurityProperties securityProperties,
                                MasterInitializer masterInitializer,
                                IkarosPluginManager pluginManager) {
        this.environment = environment;
        this.ikarosProperties = ikarosProperties;
        this.securityProperties = securityProperties;
        this.masterInitializer = masterInitializer;
        this.pluginManager = pluginManager;
        this.messageBundles = DISPLAY_LOCALES.stream()
            .map(locale -> ResourceBundle.getBundle(MESSAGE_BUNDLE_NAME, locale))
            .toList();
    }

    /**
     * 输出启动摘要.
     */
    public void log() {
        log.info("\n{}", buildSummary());
    }

    String buildSummary() {
        List<List<String>> sections = new ArrayList<>();
        sections.add(List.of("✓ " + message("startup.success")));

        List<String> serviceSection = new ArrayList<>();
        serviceSection.add(message("startup.service-address") + ": " + serviceAddress());
        masterInitializer.getInitialPassword().ifPresent(password -> {
            serviceSection.add(message("startup.initial-account") + ": "
                + securityProperties.getInitializer().getMasterUsername());
            serviceSection.add(message("startup.initial-password") + ": " + password);
            serviceSection.add("⚠ " + message("startup.password-warning"));
        });
        sections.add(serviceSection);

        List<String> pluginLines = pluginManager.getPlugins().stream()
            .sorted(Comparator.comparing(PluginWrapper::getPluginId))
            .map(this::formatPlugin)
            .toList();
        if (!pluginLines.isEmpty()) {
            List<String> pluginSection = new ArrayList<>();
            pluginSection.add(message("startup.installed-plugins"));
            pluginSection.addAll(pluginLines);
            sections.add(pluginSection);
        }

        return renderBox(sections);
    }

    private String serviceAddress() {
        int configuredPort = environment.getProperty("server.port", Integer.class, 8080);
        int actualPort = environment.getProperty("local.server.port", Integer.class,
            configuredPort);
        return UriComponentsBuilder
            .fromUri(Objects.requireNonNull(ikarosProperties.getExternalUrl()))
            .port(actualPort)
            .build()
            .toUriString();
    }

    private String message(String key) {
        return messageBundles.stream()
            .map(bundle -> bundle.getString(key))
            .collect(Collectors.joining(" / "));
    }

    private String formatPlugin(PluginWrapper pluginWrapper) {
        String pluginId = pluginWrapper.getPluginId();
        String displayName = pluginId;
        if (pluginWrapper.getDescriptor() instanceof IkarosPluginDescriptor descriptor
            && StringUtils.hasText(descriptor.getDisplayName())) {
            displayName = Objects.requireNonNull(descriptor.getDisplayName());
        }
        String name = displayName.equals(pluginId)
            ? pluginId
            : displayName + " (" + pluginId + ")";
        return "• " + name + " [v" + pluginWrapper.getDescriptor().getVersion() + "]";
    }

    private String renderBox(List<List<String>> sections) {
        int contentWidth = sections.stream()
            .flatMap(List::stream)
            .mapToInt(this::displayWidth)
            .max()
            .orElse(0);
        String horizontalLine = "═".repeat(contentWidth + 2);
        StringBuilder result = new StringBuilder()
            .append('╔').append(horizontalLine).append('╗');
        for (int sectionIndex = 0; sectionIndex < sections.size(); sectionIndex++) {
            if (sectionIndex > 0) {
                result.append('\n').append('╠').append(horizontalLine).append('╣');
            }
            for (String line : sections.get(sectionIndex)) {
                result.append('\n').append("║ ")
                    .append(line)
                    .append(" ".repeat(contentWidth - displayWidth(line)))
                    .append(" ║");
            }
        }
        return result.append('\n').append('╚').append(horizontalLine).append('╝').toString();
    }

    private int displayWidth(String value) {
        return value.codePoints()
            .map(codePoint -> isWideCharacter(codePoint) ? 2 : 1)
            .sum();
    }

    private boolean isWideCharacter(int codePoint) {
        return codePoint >= 0x1100 && codePoint <= 0x115f
            || codePoint >= 0x2e80 && codePoint <= 0xa4cf && codePoint != 0x303f
            || codePoint >= 0xac00 && codePoint <= 0xd7a3
            || codePoint >= 0xf900 && codePoint <= 0xfaff
            || codePoint >= 0xfe10 && codePoint <= 0xfe19
            || codePoint >= 0xfe30 && codePoint <= 0xfe6f
            || codePoint >= 0xff00 && codePoint <= 0xff60
            || codePoint >= 0xffe0 && codePoint <= 0xffe6
            || codePoint >= 0x1b000 && codePoint <= 0x1b2ff
            || codePoint >= 0x20000 && codePoint <= 0x3fffd;
    }
}
