package run.ikaros.server.security;

import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import run.ikaros.api.store.entity.Authority;

import static run.ikaros.api.constant.SecurityConst.AUTHORITY_DIVIDE;

public record IkarosGrantedAuthority(Authority authority) implements GrantedAuthority {
    @Override
    public @Nullable String getAuthority() {
        return authority.getType()
                + AUTHORITY_DIVIDE + authority.getTarget()
                + AUTHORITY_DIVIDE + authority.getAuthority();
    }
}
