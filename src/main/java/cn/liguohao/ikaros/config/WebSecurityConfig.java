package cn.liguohao.ikaros.config;

import cn.liguohao.ikaros.config.security.IkarosAccessDeniedHandler;
import cn.liguohao.ikaros.config.security.IkarosAuthenticationEntryPoint;
import cn.liguohao.ikaros.config.security.JwtAuthorizationFilter;
import cn.liguohao.ikaros.config.security.JwtConfig;
import cn.liguohao.ikaros.constants.SecurityConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * @author li-guohao
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    private static final String[] AUTH_WHITELIST = {
        "/swagger-ui/",
        "/swagger-ui/**",
        "/swagger-resources",
        "/swagger-resources/**",
        "/v3/api-docs",

        "/user/register",
        "/user/login",
        "/page",
        "/"

    };

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
    public void configure(WebSecurity web) {
        web.ignoring()
            .antMatchers(HttpMethod.OPTIONS, "/**")
            .antMatchers("/app/**/*.{js,html}")
            .antMatchers("/v2/api-docs/**")
            .antMatchers("/i18n/**")
            .antMatchers("/h2")
            .antMatchers("/content/**")
            .antMatchers("/webjars/springfox-swagger-ui/**")
            .antMatchers("/swagger-resources/**")
            .antMatchers("/swagger-ui.html");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
            .exceptionHandling()
            .authenticationEntryPoint(new IkarosAuthenticationEntryPoint())
            .accessDeniedHandler(new IkarosAccessDeniedHandler())
            .and()
            .csrf().disable()
            .headers().frameOptions().disable()
            .and()
            .logout().logoutUrl(SecurityConstants.API_AUTH_LOGOUT_URL).and()
            .authorizeRequests()
            // 指定路径下的资源需要进行验证后才能访问
            .antMatchers("/").permitAll()
            // 配置登录地址
            .antMatchers(HttpMethod.POST, SecurityConstants.API_AUTH_LOGIN_URL).permitAll()
            .antMatchers(HttpMethod.POST, SecurityConstants.API_USER_REGISTER_URL).permitAll()
            .antMatchers(HttpMethod.GET, SecurityConstants.API_STATUS_URLS).permitAll()
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
