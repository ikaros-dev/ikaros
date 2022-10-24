package run.ikaros.server.constants;

import org.springframework.security.access.vote.RoleVoter;

/**
 * @author li-guohao
 */
public interface UserConst {
    String DEFAULT_MASTER_USERNAME = "admin";
    String DEFAULT_MASTER_EMAIL = "admin@ikaros.run";
    String DEFAULT_MASTER_PASSWORD = "admin";
    Long UID_WHEN_NO_AUTH = 0L;
    /**
     * @see RoleVoter#rolePrefix
     */
    String SECURITY_ROLE_PREFIX = "ROLE_";
}
