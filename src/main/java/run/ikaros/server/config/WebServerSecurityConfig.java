package run.ikaros.server.config;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers.pathMatchers;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.security.DefaultUserDetailService;
import run.ikaros.server.security.authentication.SecurityConfigurer;

@Configuration
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
            .formLogin(withDefaults())
            .logout(withDefaults())
            .httpBasic(withDefaults());

        // integrate with other configurers separately
        securityConfigurers.orderedStream()
            .forEach(securityConfigurer -> securityConfigurer.configure(http));

        return http.build();
    }
}
