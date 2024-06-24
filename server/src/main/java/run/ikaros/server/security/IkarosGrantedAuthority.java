package run.ikaros.server.security;

import org.springframework.security.core.GrantedAuthority;

public record IkarosGrantedAuthority(String authority) implements GrantedAuthority {
    @Override
    public String getAuthority() {
        return this.authority;
    }
}
