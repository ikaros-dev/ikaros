package run.ikaros.server.security;

import static run.ikaros.api.constant.SecurityConst.AUTHORITY_DIVIDE;

import org.springframework.security.core.GrantedAuthority;
import run.ikaros.api.core.authority.Authority;
import run.ikaros.api.store.enums.AuthorityType;

public record IkarosGrantedAuthority(Authority authority)
    implements GrantedAuthority {

    @Override
    public String getAuthority() {
        AuthorityType type = authority.getType();
        if (type == null) {
            throw new NullPointerException();
        }
        return type.name()
            + AUTHORITY_DIVIDE + authority.getTarget()
            + AUTHORITY_DIVIDE + authority.getAuthority();
    }
}
