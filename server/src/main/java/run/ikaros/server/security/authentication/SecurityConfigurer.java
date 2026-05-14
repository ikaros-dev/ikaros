package run.ikaros.server.security.authentication;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;

public interface SecurityConfigurer {
    void configure(HttpSecurity http);
}
