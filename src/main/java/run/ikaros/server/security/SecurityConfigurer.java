package run.ikaros.server.security;

import org.springframework.security.config.web.server.ServerHttpSecurity;

public interface SecurityConfigurer {
    void configure(ServerHttpSecurity http);
}
