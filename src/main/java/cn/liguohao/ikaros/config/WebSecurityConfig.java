package cn.liguohao.ikaros.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

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

    /**
     * 配置安全策略
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            // 关闭csrf跨域检查
            .csrf().disable()
            .authorizeRequests()
            .antMatchers(AUTH_WHITELIST).permitAll()
            .anyRequest().authenticated()
            .and()
            .formLogin()
            .loginPage("/page/login")
            .loginProcessingUrl("/user/login")
            .failureUrl("/page/login-error")
            .permitAll();
    }
}
