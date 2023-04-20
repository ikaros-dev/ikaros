package run.ikaros.server.plugin;

import jakarta.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.Extension;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.AnnotationTypeFilter;

@Slf4j
public class PluginExtensionAnnotationPostProcessor implements BeanDefinitionRegistryPostProcessor {
    private final String basePackage;

    public PluginExtensionAnnotationPostProcessor(String basePackage) {
        this.basePackage = basePackage;
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry)
        throws BeansException {
        ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Extension.class));
        int scan = scanner.scan(basePackage);
        log.debug("scan @Extension class size: {} in basePackage: {}", scan, basePackage);
    }

    @Override
    public void postProcessBeanFactory(@Nonnull ConfigurableListableBeanFactory beanFactory)
        throws BeansException {
    }
}
