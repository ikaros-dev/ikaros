package run.ikaros.server.security;

import static run.ikaros.api.constant.SecurityConst.AUTHORITY_DIVIDE;

import org.springframework.security.core.GrantedAuthority;
import run.ikaros.api.core.authority.Authority;

public record IkarosGrantedAuthority(Authority authority)
    implements GrantedAuthority {

    @Override
    public String getAuthority() {
        return authority.getType().name()
            + AUTHORITY_DIVIDE + authority.getTarget()
            + AUTHORITY_DIVIDE + authority.getAuthority();
    }
}
