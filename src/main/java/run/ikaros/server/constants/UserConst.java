package run.ikaros.server.constants;

import org.springframework.security.access.vote.RoleVoter;

/**
 * @author li-guohao
 */
public interface UserConst {
    Long UID_WHEN_NO_AUTH = 0L;
    /**
     * @see RoleVoter#rolePrefix
     */
    String SECURITY_ROLE_PREFIX = "ROLE_";
    String HIDDEN_STR = "**hidden**";
    String DEFAULT_ROLE = "ADMIN";
}
