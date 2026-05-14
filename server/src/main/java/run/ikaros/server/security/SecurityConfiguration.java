package run.ikaros.server.security;


import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.util.matcher.AndServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.MediaTypeServerWebExchangeMatcher;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.server.security.authentication.SecurityConfigurer;
import run.ikaros.server.security.authorization.RequestAuthorizationManager;
import run.ikaros.server.store.mapper.*;

import java.util.Set;

import static org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

@EnableWebSecurity
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(UserMapper userMapper, UserRoleMapper userRoleMapper,
                                          RoleMapper roleMapper, RoleAuthorityMapper roleAuthorityMapper,
                                          AuthorityMapper authorityMapper) {
        return new DefaultUserDetailService(userMapper, userRoleMapper, roleMapper,
                roleAuthorityMapper, authorityMapper);
    }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    SecurityFilterChain apiFilterChain(HttpSecurity http,
                                       ObjectProvider<SecurityConfigurer> securityConfigurers) {
        // main config
        http.authorizeHttpRequests(authorize -> authorize
                // 静态资源放行
                .requestMatchers("/css/**", "/js/**", "/images/**").permitAll()
                // 登录页面放行
                .requestMatchers("/login", "/register", "/logout").permitAll()
                // 所有其他请求使用自定义 AuthorizationManager
                .anyRequest()
                .access(new RequestAuthorizationManager())
        );

        // integrate with other configurers separately
        securityConfigurers.orderedStream()
                .forEach(securityConfigurer -> securityConfigurer.configure(http));

        return http.build();
    }

//    @Bean
//    @Order(Ordered.HIGHEST_PRECEDENCE + 1)
//    SecurityFilterChain portalFilterChain(HttpSecurity http) {
//        var pathMatcher = pathMatchers(HttpMethod.GET, "/**");
//        var mediaTypeMatcher = new MediaTypeServerWebExchangeMatcher(MediaType.TEXT_HTML);
//        mediaTypeMatcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
//        http.securityMatcher(new AndServerWebExchangeMatcher(pathMatcher, mediaTypeMatcher))
//                .authorizeExchange(authorizeExchangeSpec ->
//                        authorizeExchangeSpec.anyExchange().permitAll())
//                .headers(headerSpec ->
//                        headerSpec.frameOptions(frameOptionsSpec -> frameOptionsSpec.mode(SAMEORIGIN))
//                                .referrerPolicy(referrerPolicySpec -> referrerPolicySpec.policy(
//                                        STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
//                                .cache(ServerHttpSecurity.HeaderSpec.CacheSpec::disable)
//                ).anonymous(anonymousSpec -> {
//                    anonymousSpec.authorities(SecurityConst.AnonymousUser.Role);
//                    anonymousSpec.principal(SecurityConst.AnonymousUser.PRINCIPAL);
//                });
//        return http.build();
//    }
}
