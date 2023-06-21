package run.ikaros.server.plugin.listener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginDependency;
import org.pf4j.PluginDescriptor;
import org.pf4j.PluginWrapper;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import run.ikaros.api.core.setting.ConfigMap;
import run.ikaros.api.custom.ReactiveCustomClient;
import run.ikaros.api.exception.NotFoundException;
import run.ikaros.api.plugin.custom.Plugin;
import run.ikaros.server.infra.utils.StringUtils;
import run.ikaros.server.plugin.IkarosPluginDescriptor;
import run.ikaros.server.plugin.IkarosPluginManager;
import run.ikaros.server.plugin.resource.BundleResourceUtils;

@Slf4j
public class PluginDatabaseUtils {

    /**
     * Save plugin to database.
     *
     * @param pluginId      plugin name(id)
     * @param pluginManager ikaros plugin manager
     * @param customClient  reactive custom client
     * @return database plugin record
     */
    public static Mono<Plugin> savePluginToDatabase(String pluginId,
                                                    IkarosPluginManager pluginManager,
                                                    ReactiveCustomClient customClient) {
        PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
        IkarosPluginDescriptor pluginDescriptor =
            (IkarosPluginDescriptor) pluginWrapper.getDescriptor();
        Plugin plugin = new Plugin();
        plugin.setName(pluginId);
        plugin.setState(pluginWrapper.getPluginState());
        plugin.setDescription(pluginDescriptor.getPluginDescription());
        plugin.setVersion(pluginDescriptor.getVersion());
        plugin.setRequires(pluginDescriptor.getRequires());
        Plugin.Author author = new Plugin.Author();
        author.setName(pluginDescriptor.getProvider());
        plugin.setAuthor(author);
        plugin.setLicense(pluginDescriptor.getLicense());
        plugin.setLoadLocation(pluginWrapper.getPluginPath());
        plugin.setClazz(pluginWrapper.getClass().getName());
        plugin.setDependencies(
            convertPluginDependencies(pluginDescriptor.getDependencies(), pluginManager));
        plugin.setDisplayName(pluginDescriptor.getDisplayName());
        plugin.setAuthor(pluginDescriptor.getAuthor());
        plugin.setHomepage(pluginDescriptor.getHomepage());
        plugin.setLogo(pluginDescriptor.getLogo());
        plugin.setLoadLocation(pluginDescriptor.getLoadLocation());
        plugin.setConfigMapSchemas(pluginDescriptor.getConfigMapSchemas());
        setPluginStaticResourceIfExists(pluginId, plugin, pluginManager);

        if (StringUtils.isNotBlank(plugin.getConfigMapSchemas())) {
            savePluginConfigMap(plugin.getConfigMapSchemas(), customClient, pluginId)
                .subscribeOn(Schedulers.boundedElastic()).subscribe();
        }

        return customClient.findOne(Plugin.class, pluginId)
            .onErrorResume(NotFoundException.class, e -> customClient.create(plugin)
                .doOnSuccess(p -> log.debug("Create new plugin record for name: [{}].", pluginId)));
    }

    private static Mono<Void> savePluginConfigMap(String configMapSchemas,
                                                  ReactiveCustomClient customClient,
                                                  String pluginId) {
        Assert.hasText(configMapSchemas, "'configMapSchemas' must has text.");
        Assert.hasText(pluginId, "'pluginId' must has text.");
        Assert.notNull(customClient, "'customClient' must not null.");

        ConfigMap configMap = new ConfigMap();
        configMap.setName(pluginId);

        // 创建一个正则表达式模式，匹配"name: 'value'"的形式
        Pattern pattern = Pattern.compile("\"name\"\\s*:\\s*\"(.*?)\"");

        // 创建一个Matcher对象，用于在输入字符串中进行匹配
        Matcher matcher = pattern.matcher(configMapSchemas);

        // 循环查找匹配项并提取name值
        while (matcher.find()) {
            String name = matcher.group(1); // 获取第一个捕获组的值
            configMap.putDataItem(name, "");
        }

        return customClient.findOne(ConfigMap.class, pluginId)
            .onErrorResume(NotFoundException.class, e -> customClient.create(configMap)
                .doOnSuccess(cm -> log.info("save plugin:[{}] config map:[{}].", pluginId, cm)))
            .then();
    }

    /**
     * Convert plugin dependencies.
     *
     * @see Plugin#getDependencies()
     */
    private static Map<String, String> convertPluginDependencies(
        List<PluginDependency> dependencies, IkarosPluginManager pluginManager) {
        Map<String, String> pluginIdVersionMap = new HashMap<>();
        for (PluginDependency dependency : dependencies) {
            String pluginId = dependency.getPluginId();
            PluginWrapper pluginWrapper = pluginManager.getPlugin(pluginId);
            PluginDescriptor descriptor = pluginWrapper.getDescriptor();
            String version = descriptor.getVersion();
            pluginIdVersionMap.put(pluginId, version);
        }
        return pluginIdVersionMap;
    }

    private static void setPluginStaticResourceIfExists(String pluginId, Plugin plugin,
                                                        IkarosPluginManager pluginManager) {
        // Console bundle entry js.
        String jsBundlePath = BundleResourceUtils.getJsBundlePath(pluginManager, pluginId);
        plugin.setEntry(jsBundlePath);
        // Console bundle style css.
        String cssBundlePath =
            BundleResourceUtils.getCssBundlePath(pluginManager, plugin.getName());
        plugin.setStylesheet(cssBundlePath);
    }


}
