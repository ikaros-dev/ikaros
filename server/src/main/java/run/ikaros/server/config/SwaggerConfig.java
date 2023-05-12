package run.ikaros.server.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class SwaggerConfig {
    /**
     * config core openapi group.
     *
     * @return core openapi group
     */
    @Bean
    public GroupedOpenApi coreApiDocket() {
        return GroupedOpenApi.builder()
            .group("CoreOpenApi")
            .pathsToMatch("/api/**")
            .build();
    }

    /**
     * config custom openapi group.
     *
     * @return custom openapi group
     */
    @Bean
    public GroupedOpenApi customApiDocket() {
        return GroupedOpenApi.builder()
            .group("CustomOpenApi")
            .pathsToMatch("/apis/**")
            .build();
    }

    /**
     * config all openapi group.
     *
     * @return all openapi group
     */
    @Bean
    public GroupedOpenApi allApiDocket() {
        return GroupedOpenApi.builder()
            .group("AllOpenApi")
            .pathsToMatch("/**")
            .build();
    }

    /**
     * ikaros openapi description info.
     *
     * @return openapi instance
     */
    @Bean
    public OpenAPI ikarosOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("Ikaros Open API Documentation")
                .description("Documentation for Ikaros Open API")
                .version("1.0.0")
                .license(new License().name("AGPL-3.0 license")
                    .url("https://github.com/ikaros-dev/ikaros/blob/master/LICENSE")))
            .externalDocs(new ExternalDocumentation()
                .description("Ikaros Official Site")
                .url("https://ikaros.run"))
            .components(new Components()
                .addSecuritySchemes("BasicAuth",
                    new SecurityScheme()
                        .name("BasicAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("Basic"))
                .addSecuritySchemes("BearerAuth",
                    new SecurityScheme()
                        .name("BearerAuth")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("Bearer")
                        .bearerFormat("JWT"))
            )
            .addSecurityItem(new SecurityRequirement()
                .addList("BasicAuth").addList("BearerAuth")
            );
    }

}
