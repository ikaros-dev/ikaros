package run.ikaros.server.config;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.ApplicationContext;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import run.ikaros.api.core.attachment.AttachmentConst;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.server.console.ConsoleProperties;

/** 验证附件驱动静态文件出口已从 WebFlux 资源映射中移除. */
class WebFluxConfigTest {
    @Test
    void addResourceHandlers_doesNotRegisterDriverStaticResource(@TempDir Path workDir) {
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        IkarosProperties ikarosProperties = mock(IkarosProperties.class);
        ConsoleProperties consoleProperties = mock(ConsoleProperties.class);
        whenWorkDir(ikarosProperties, workDir);
        whenConsoleLocation(consoleProperties);
        WebFluxConfig config = new WebFluxConfig(
            applicationContext, ikarosProperties, consoleProperties);
        ResourceHandlerRegistry registry = mock(ResourceHandlerRegistry.class, RETURNS_DEEP_STUBS);

        config.addResourceHandlers(registry);

        verify(registry, never()).addResourceHandler(
            AttachmentConst.DRIVER_STATIC_RESOURCE_PREFIX + "/**");
    }

    private void whenWorkDir(IkarosProperties properties, Path workDir) {
        org.mockito.Mockito.when(properties.getWorkDir()).thenReturn(workDir);
    }

    private void whenConsoleLocation(ConsoleProperties properties) {
        org.mockito.Mockito.when(properties.getLocation()).thenReturn("classpath:/console/");
    }
}
