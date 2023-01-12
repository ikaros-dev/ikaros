package run.ikaros.server.plugin;

import jakarta.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.ExtensionFactory;
import org.pf4j.Plugin;
import org.pf4j.PluginManager;
import org.pf4j.PluginWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.util.Assert;

/**
 * <p>Basic implementation for {@link ExtensionFactory}.</p>
 * <p>Uses Springs {@link AutowireCapableBeanFactory} to instantiate a given extension class,
 * so can support {@link Autowired}.</p>
 *
 * @author li-guohao
 */
@Slf4j
public class IkarosExtensionFactory implements ExtensionFactory {
    public static final boolean AUTOWIRE_BY_DEFAULT = true;
    protected final PluginManager pluginManager;
    protected final boolean autowire;

    public IkarosExtensionFactory(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        this.autowire = AUTOWIRE_BY_DEFAULT;
    }

    public IkarosExtensionFactory(PluginManager pluginManager, boolean autowire) {
        this.pluginManager = pluginManager;
        this.autowire = autowire;
    }

    @Override
    public <T> T create(Class<T> extensionClass) {
        Assert.notNull(extensionClass, "'extensionClass' must not be null");
        if (!this.autowire) {
            log.warn("Create instance of '" + extensionClass.getName()
                + "' without using springs possibilities as"
                + " autowiring is disabled.");
            return createWithoutSpring(extensionClass);
        }
        Optional<PluginApplicationContext> contextOptional =
            getPluginApplicationContextBy(extensionClass);
        if (contextOptional.isPresent()) {
            // When the plugin starts, the class has been loaded into the plugin application
            // context,
            // so you only need to get it directly
            PluginApplicationContext pluginApplicationContext = contextOptional.get();
            return pluginApplicationContext.getBean(extensionClass);
        }
        return createWithoutSpring(extensionClass);
    }

    private <T> Optional<PluginApplicationContext> getPluginApplicationContextBy(
        Class<T> extensionClass) {
        final Plugin plugin = Optional.ofNullable(this.pluginManager.whichPlugin(extensionClass))
            .map(PluginWrapper::getPlugin)
            .orElse(null);

        final PluginApplicationContext applicationContext;

        if (plugin instanceof BasePlugin) {
            log.debug(
                "  Extension class ' " + extensionClass.getName() + "' belongs to ikaros-plugin '"
                    + nameOf(plugin)
                    + "' and will be autowired by using its application context.");
            applicationContext = PluginApplicationContextRegistry.getInstance()
                .getByPluginId(plugin.getWrapper().getPluginId());
            return Optional.of(applicationContext);
        } else if (this.pluginManager instanceof IkarosPluginManager && plugin != null) {
            log.debug("  Extension class ' " + extensionClass.getName()
                + "' belongs to a non ikaros-plugin (or main application)"
                + " '" + nameOf(plugin)
                + ", but the used ikaros plugin-manager is a spring-plugin-manager. Therefore"
                + " the extension class will be autowired by using the managers application "
                + "contexts");
            String pluginId = plugin.getWrapper().getPluginId();
            applicationContext = ((IkarosPluginManager) this.pluginManager)
                .getPluginApplicationContext(pluginId);
        } else {
            log.warn("  No application contexts can be used for instantiating extension class '"
                + extensionClass.getName() + "'."
                + " This extension neither belongs to a halo-plugin (id: '" + nameOf(plugin)
                + "') nor is the used"
                + " plugin manager a spring-plugin-manager (used manager: '"
                + this.pluginManager.getClass().getName()
                + "')."
                + " At perspective of PF4J this seems highly uncommon in combination with a factory"
                + " which only reason for existence"
                + " is using spring (and its application context) and should at least be reviewed. "
                + "In fact no autowiring can be"
                + " applied although autowire flag was set to 'true'. Instantiating will fallback "
                + "to standard Java reflection.");
            applicationContext = null;
        }

        return Optional.ofNullable(applicationContext);
    }


    @Nullable
    protected <T> T createWithoutSpring(final Class<T> extensionClass) {
        Assert.notNull(extensionClass, "'extensionClass' must not be null");
        T t;
        try {
            t = extensionClass.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException
                 | InvocationTargetException | NoSuchMethodException e) {
            throw new PluginException("new extension cls instance fail", e);
        }
        return t;
    }

    private String nameOf(final Plugin plugin) {
        return Objects.nonNull(plugin)
            ? plugin.getWrapper().getPluginId()
            : "system";
    }

}
