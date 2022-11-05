package run.ikaros.server.config;

import run.ikaros.server.constants.AppConst;
import run.ikaros.server.utils.StringUtils;
import run.ikaros.server.constants.SecurityConst;
import run.ikaros.server.utils.JwtUtils;
import run.ikaros.server.config.security.IkarosAccessDeniedHandler;
import run.ikaros.server.config.security.IkarosAuthenticationEntryPoint;
import run.ikaros.server.config.security.JwtAuthorizationFilter;
import run.ikaros.server.config.security.JwtConfig;
import run.ikaros.server.exceptions.JwtTokenValidateFailException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextPersistenceFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * @author li-guohao
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(WebSecurityConfig.class);


    private final CorsFilter corsFilter;

    public WebSecurityConfig(CorsFilter corsFilter) {
        this.corsFilter = corsFilter;
    }


    /**
     * 用户名密码认证管理器
     */
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }


    /**
     * 注入密码加密解密器
     */
    @Bean
    public PasswordEncoder injectPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(corsFilter, SecurityContextPersistenceFilter.class)
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
            .authorizeRequests()
            // 指定路径下的资源需要进行验证后才能访问
            .antMatchers(HttpMethod.POST, SecurityConst.API_AUTH_LOGIN_URL).permitAll()
            .antMatchers(HttpMethod.POST, SecurityConst.API_USER_REGISTER_URL).permitAll()
            .antMatchers(HttpMethod.GET, "/static/**").permitAll()
            .antMatchers(HttpMethod.GET, "/upload/**").permitAll()
            .antMatchers(HttpMethod.OPTIONS).permitAll()
            .antMatchers(AppConst.OpenAPI.PREFIX_NAME + "/**").authenticated()
            // 其他请求需验证
            .anyRequest().permitAll()
            .and()
            // 不需要 session（不创建会话）
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .apply(securityConfigurationAdapter());
    }

    private JwtConfig securityConfigurationAdapter() throws Exception {
        return new JwtConfig(new JwtAuthorizationFilter(authenticationManager()));
    }
}
