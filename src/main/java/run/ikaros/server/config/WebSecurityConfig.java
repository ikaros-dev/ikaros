package run.ikaros.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.filter.CorsFilter;
import run.ikaros.server.config.security.IkarosAccessDeniedHandler;
import run.ikaros.server.config.security.IkarosAuthenticationEntryPoint;
import run.ikaros.server.config.security.JwtAuthorizationFilter;
import run.ikaros.server.config.security.JwtConfig;
import run.ikaros.server.constants.AppConst;
import run.ikaros.server.constants.SecurityConst;
import run.ikaros.server.exceptions.JwtTokenValidateFailException;
import run.ikaros.server.utils.JwtUtils;
import run.ikaros.server.utils.StringUtils;

/**
 * @author li-guohao
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityConfig.class);


    private final CorsFilter corsFilter;

    public WebSecurityConfig(CorsFilter corsFilter) {
        this.corsFilter = corsFilter;
    }

    /**
     * 注入密码加密解密器
     */
    @Bean
    public PasswordEncoder injectPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
        AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(corsFilter, SecurityContextHolderFilter.class)
            .exceptionHandling()
            .authenticationEntryPoint(new IkarosAuthenticationEntryPoint())
            .accessDeniedHandler(new IkarosAccessDeniedHandler())
            .and()
            .csrf().disable()
            .headers().frameOptions().disable()
            .and()
            .logout()
            .logoutUrl(SecurityConst.API_AUTH_LOGOUT_URL)
            .clearAuthentication(true)
            .logoutSuccessHandler((request, response, authentication) -> {
                String token = request.getHeader(SecurityConst.TOKEN_HEADER);
                if (StringUtils.isNotBlank(token)) {
                    token = token.replace(SecurityConst.TOKEN_PREFIX, "");
                    if (!JwtUtils.validateToken(token)) {
                        throw new JwtTokenValidateFailException(
                            "validate fail for token: " + token);
                    }
                    Authentication auth = JwtUtils.getAuthentication(token);
                    LOGGER.debug("logout success, username: {}", auth.getPrincipal());
                    // todo let token expire
                    // can use cache such as userId ==> token, expireTime
                }
            })
            .and()
            .authorizeHttpRequests()
            // 指定路径下的资源需要进行验证后才能访问
            .requestMatchers(HttpMethod.POST, SecurityConst.API_AUTH_LOGIN_URL).permitAll()
            .requestMatchers(HttpMethod.POST, SecurityConst.API_USER_REGISTER_URL).permitAll()
            .requestMatchers(HttpMethod.GET, SecurityConst.API_AUTH_OPTION_IS_INIT).permitAll()
            .requestMatchers(HttpMethod.POST, SecurityConst.API_AUTH_OPTION_APP_INIT).permitAll()
            .requestMatchers(HttpMethod.GET, "/static/**").permitAll()
            .requestMatchers(HttpMethod.GET, "/upload/**").permitAll()
            .requestMatchers(HttpMethod.OPTIONS).permitAll()
            .requestMatchers(AppConst.OpenAPI.PREFIX_NAME + "/**").authenticated()
            // 其他请求需验证
            .anyRequest().permitAll()
            .and()
            // 不需要 session（不创建会话）
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .apply(securityConfigurationAdapter(
                authenticationManager(http.getSharedObject(AuthenticationConfiguration.class))));
        return http.build();
    }

    private JwtConfig securityConfigurationAdapter(AuthenticationManager authenticationManager) {
        return new JwtConfig(new JwtAuthorizationFilter(authenticationManager));
    }
}
