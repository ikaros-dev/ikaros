package run.ikaros.server.config;

import java.io.File;
import java.nio.file.Path;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import run.ikaros.api.constant.AppConst;
import run.ikaros.api.infra.properties.IkarosProperties;

@Configuration
public class ThemeConfig {

    private final SpringTemplateEngine templateEngine;
    private final IkarosProperties ikarosProperties;

    public ThemeConfig(SpringTemplateEngine templateEngine, IkarosProperties ikarosProperties) {
        this.templateEngine = templateEngine;
        this.ikarosProperties = ikarosProperties;
    }

    /**
     * Add thymeleaf theme extension for user themes dir.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void thymeleafExtension() {
        if (!templateEngine.isInitialized()) {
            Path workDir = ikarosProperties.getWorkDir();
            Path themesDir = workDir.resolve(AppConst.THEME_DIR_NAME);
            FileTemplateResolver resolver = new FileTemplateResolver();
            resolver.setCheckExistence(true);
            resolver.setPrefix(themesDir.toString() + File.separatorChar);
            resolver.setSuffix(".html");
            resolver.setTemplateMode(TemplateMode.HTML);
            resolver.setOrder(templateEngine.getTemplateResolvers().size());
            resolver.setCacheable(false);
            templateEngine.addTemplateResolver(resolver);
        }
    }
}
