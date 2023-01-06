package run.ikaros.server.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN;
import static org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.MediaTypeServerWebExchangeMatcher;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.infra.constant.SecurityConst;
import run.ikaros.server.infra.properties.IkarosProperties;
import run.ikaros.server.security.DefaultUserDetailService;
import run.ikaros.server.security.MasterInitializer;
import run.ikaros.server.security.authentication.SecurityConfigurer;
import run.ikaros.server.security.authorization.RequestAuthorizationManager;

@Configuration
@EnableWebFluxSecurity
public class WebServerSecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    ReactiveUserDetailsService userDetailsService(UserService userService) {
        return new DefaultUserDetailService(userService);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityWebFilterChain apiFilterChain(ServerHttpSecurity http,
                                          ObjectProvider<SecurityConfigurer> securityConfigurers) {
        // main config
        http.securityMatcher(pathMatchers("/api/**", "/apis/**", "/login", "/logout"))
            .authorizeExchange().anyExchange()
            .access(new RequestAuthorizationManager())
            .and()
            .formLogin(withDefaults())
            .logout(withDefaults())
            .httpBasic(withDefaults());

        // integrate with other configurers separately
        securityConfigurers.orderedStream()
            .forEach(securityConfigurer -> securityConfigurer.configure(http));

        return http.build();
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
    SecurityWebFilterChain portalFilterChain(ServerHttpSecurity http) {
        var pathMatcher = pathMatchers(HttpMethod.GET, "/**");
        var mediaTypeMatcher = new MediaTypeServerWebExchangeMatcher(MediaType.TEXT_HTML);
        mediaTypeMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        http.securityMatcher(new AndServerWebExchangeMatcher(pathMatcher, mediaTypeMatcher))
            .authorizeExchange().anyExchange().permitAll().and()
            .headers()
            .frameOptions().mode(SAMEORIGIN)
            .referrerPolicy().policy(STRICT_ORIGIN_WHEN_CROSS_ORIGIN).and()
            .cache().disable().and()
            .anonymous(spec -> {
                spec.authorities(SecurityConst.AnonymousUser.Role);
                spec.principal(SecurityConst.AnonymousUser.PRINCIPAL);
            });
        return http.build();
    }

    @Bean
    @ConditionalOnProperty(name = "ikaros.security.initializer.disabled",
        havingValue = "false",
        matchIfMissing = true)
    MasterInitializer superAdminInitializer(IkarosProperties ikarosProperties,
                                            UserService userService) {
        return new MasterInitializer(ikarosProperties.getSecurity().getInitializer(), userService);
    }

}
