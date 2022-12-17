package run.ikaros.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import run.ikaros.server.constants.AppConst;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.Contact;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

import java.util.ArrayList;
import java.util.List;

/**
 * @author guohao
 * @date 2022/09/11
 */
@Configuration
@EnableOpenApi
public class SwaggerConfig {

    @Bean
    public Docket openApiDocket() {
        return new Docket(DocumentationType.OAS_30)
            .apiInfo(apiInfo())
            .groupName("ikaros-openApi")
            .enable(true)
            .select()
            .apis(RequestHandlerSelectors.basePackage(AppConst.OpenAPI.PACKAGE_NAME))
            .build()
            .securitySchemes(securitySchemes())
            .securityContexts(securityContexts());
    }

    private List<SecurityScheme> securitySchemes() {
        List<SecurityScheme> apiKeyList = new ArrayList<SecurityScheme>();
        apiKeyList.add(new ApiKey("Authorization", "user-token", "header"));
        return apiKeyList;
    }

    private List<SecurityContext> securityContexts() {
        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(
            SecurityContext.builder()
                .securityReferences(defaultAuth())
                //.forPaths(PathSelectors.regex("^(?!auth).*$"))
                .build());
        return securityContexts;
    }

    List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope =
            new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        List<SecurityReference> securityReferences = new ArrayList<>();
        securityReferences.add(new SecurityReference("Authorization", authorizationScopes));
        return securityReferences;
    }


    private ApiInfo apiInfo() {
        Contact contact = new Contact("li-guohao", "https://github.com/li-guohao/ikaros/issues",
            "git@liguohao.cn");
        return new ApiInfoBuilder()
            .title("Ikaros Open API Documentation")
            .description("Documentation for Ikaros Open API")
            .version("V1")
            .termsOfServiceUrl("https://github.com/li-guohao/ikaros")
            .contact(contact)
            .license("GNU General Public License v3.0")
            .licenseUrl("https://github.com/li-guohao/ikaros/blob/develop/LICENSE")
            .build();
    }


}
