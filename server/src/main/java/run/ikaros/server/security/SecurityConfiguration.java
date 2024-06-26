package run.ikaros.server.security;

import static org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN;
import static org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter.Mode.SAMEORIGIN;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.server.core.user.RoleService;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.security.authentication.SecurityConfigurer;
import run.ikaros.server.security.authorization.RequestAuthorizationManager;
import run.ikaros.server.store.repository.AuthorityRepository;
import run.ikaros.server.store.repository.RoleAuthorityRepository;
import run.ikaros.server.store.repository.UserRepository;
import run.ikaros.server.store.repository.UserRoleRepository;

@EnableWebFluxSecurity
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    ReactiveUserDetailsService userDetailsService(
        AuthorityRepository authorityRepository, UserRepository userRepository,
        UserRoleRepository userRoleRepository,
        RoleAuthorityRepository roleAuthorityRepository) {
        return new DefaultUserDetailService(userRepository, userRoleRepository,
            authorityRepository, roleAuthorityRepository);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityWebFilterChain apiFilterChain(ServerHttpSecurity http,
                                          ObjectProvider<SecurityConfigurer> securityConfigurers) {
        // main config
        http.securityMatcher(pathMatchers(SecurityConst.SECURITY_MATCHER_PATHS))
            .authorizeExchange().anyExchange()
            .access(new RequestAuthorizationManager())
            .and()
            .anonymous(spec -> {
                spec.authorities(SecurityConst.AnonymousUser.Role);
                spec.principal(SecurityConst.AnonymousUser.PRINCIPAL);
            });

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
    MasterInitializer superAdminInitializer(SecurityProperties securityProperties,
                                            UserService userService, RoleService roleService,
                                            UserRoleRepository userRoleRepository) {
        return new MasterInitializer(securityProperties.getInitializer(), userService, roleService,
                userRoleRepository);
    }
}
