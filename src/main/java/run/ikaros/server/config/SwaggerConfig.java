package run.ikaros.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.ikaros.server.constants.AppConst;

/**
 * @author guohao
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public GroupedOpenApi openApiDocket() {
        return GroupedOpenApi.builder()
            .group("ikaros-openApi")
            .pathsToMatch("/api/**")
            .packagesToScan(AppConst.OpenAPI.PACKAGE_NAME)
            .build();
    }

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Ikaros Open API Documentation")
                .description("Documentation for Ikaros Open API")
                .version("v0.1.0")
                .license(new License().name("AGPL-3.0 license").url("https://github.com/ikaros-dev/ikaros/blob/master/LICENSE")))
            .externalDocs(new ExternalDocumentation()
                .description("Ikaros Official Site")
                .url("https://ikaros.run"))
            .addSecurityItem(new SecurityRequirement().addList("IkarosBearerAuth"))
            .components(new Components()
                .addSecuritySchemes("IkarosBearerAuth",
                    new SecurityScheme()
                        .name("IkarosBearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("Bearer")
                        .bearerFormat("JWT")));
    }

}
