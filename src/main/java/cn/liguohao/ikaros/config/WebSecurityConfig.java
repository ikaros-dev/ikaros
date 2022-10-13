package cn.liguohao.ikaros.config;

import cn.liguohao.ikaros.common.Strings;
import cn.liguohao.ikaros.common.constants.SecurityConstants;
import cn.liguohao.ikaros.common.kit.JwtKit;
import cn.liguohao.ikaros.config.security.IkarosAccessDeniedHandler;
import cn.liguohao.ikaros.config.security.IkarosAuthenticationEntryPoint;
import cn.liguohao.ikaros.config.security.JwtAuthorizationFilter;
import cn.liguohao.ikaros.config.security.JwtConfig;
import cn.liguohao.ikaros.exceptions.JwtTokenValidateFailException;
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
            .logoutUrl(SecurityConstants.API_AUTH_LOGOUT_URL)
            .clearAuthentication(true)
            .logoutSuccessHandler((request, response, authentication) -> {
                String token = request.getHeader(SecurityConstants.TOKEN_HEADER);
                if (Strings.isNotBlank(token)) {
                    token = token.replace(SecurityConstants.TOKEN_PREFIX, "");
                    if (!JwtKit.validateToken(token)) {
                        throw new JwtTokenValidateFailException(
                            "validate fail for token: " + token);
                    }
                    Authentication auth = JwtKit.getAuthentication(token);
                    LOGGER.debug("logout success, username: {}", auth.getPrincipal());
                    // todo let token expire
                    // can use cache such as userId ==> token, expireTime
                }
            })
            .and()
            .authorizeRequests()
            // 指定路径下的资源需要进行验证后才能访问
            .antMatchers(HttpMethod.POST, SecurityConstants.API_AUTH_LOGIN_URL).permitAll()
            .antMatchers(HttpMethod.POST, SecurityConstants.API_USER_REGISTER_URL).permitAll()
            .antMatchers(HttpMethod.GET, SecurityConstants.PAGE_ADMIN_URL).permitAll()
            .antMatchers(HttpMethod.GET, "/static/**").permitAll()
            .antMatchers(HttpMethod.GET, "/upload/**").permitAll()
            .antMatchers(HttpMethod.OPTIONS).permitAll()
            .antMatchers(SecurityConstants.API_STATUS_URLS).permitAll()
            .antMatchers(SecurityConstants.SWAGGER_DOC_URLS).permitAll()
            .antMatchers(SecurityConstants.APP_URLS).permitAll()
            // 其他请求需验证
            .anyRequest().authenticated()
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
