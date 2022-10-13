package cn.liguohao.ikaros.config;

import cn.liguohao.ikaros.common.constants.AppConstants;
import cn.liguohao.ikaros.common.kit.SystemVarKit;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author liguohao
 * @date 2022/09/07
 */
@EnableWebMvc
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer config) {
        config.addPathPrefix(AppConstants.OPEN_API_PREFIX_NAME,
            HandlerTypePredicate.forAnnotation(RestController.class)
                .and(cls -> cls.getPackageName().contains(AppConstants.OPEN_API_PACKAGE_NAME)));
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // register admin
        registry.addResourceHandler("/admin/**")
            .addResourceLocations("classpath:/admin/");

        // register upload
        registry.addResourceHandler("/upload/**")
            .addResourceLocations("file:" + SystemVarKit.getCurrentAppDirPath() + File.separatorChar
                + "upload" + File.separatorChar);
    }

    @Bean
    public CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        config.setAllowedMethods(Collections.singletonList("*"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setExposedHeaders(Arrays.asList(
            "Authorization", "X-Total-Count", "Link",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }

}
