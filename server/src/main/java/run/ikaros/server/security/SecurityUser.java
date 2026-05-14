package run.ikaros.server.security;

import java.util.Collection;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import run.ikaros.api.store.entity.User;

public class SecurityUser implements UserDetails, CredentialsContainer {
    private final User user;
    private final List<IkarosGrantedAuthority> authorities;

    public SecurityUser(User user, List<IkarosGrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getNonLocked();
    }

    @Override
    public boolean isEnabled() {
        return user.getEnable();
    }

    @Override
    public void eraseCredentials() {
        user.setPassword(null);
    }

    public Long getId() {
        return user.getId();
    }
}
