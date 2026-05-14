package run.ikaros.server.security;


import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import run.ikaros.server.security.authentication.SecurityConfigurer;
import run.ikaros.server.security.authorization.RequestAuthorizationManager;
import run.ikaros.server.store.mapper.AuthorityMapper;
import run.ikaros.server.store.mapper.RoleAuthorityMapper;
import run.ikaros.server.store.mapper.RoleMapper;
import run.ikaros.server.store.mapper.UserMapper;
import run.ikaros.server.store.mapper.UserRoleMapper;

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
                                          RoleMapper roleMapper,
                                          RoleAuthorityMapper roleAuthorityMapper,
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
            // 所有请求使用自定义 AuthorizationManager
            .anyRequest()
            .access(new RequestAuthorizationManager())
        );

        // integrate with other configurers separately
        securityConfigurers.orderedStream()
            .forEach(securityConfigurer -> securityConfigurer.configure(http));

        return http.build();
    }

}
