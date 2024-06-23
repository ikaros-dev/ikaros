package run.ikaros.api.constant;

/**
 * security constants.
 *
 * @author: chivehao
 */
public interface SecurityConst {
    String PREFIX = "ROLE_";
    /**
     * 有完整的路径权限.
     */
    String ROLE_MASTER = "MASTER";
    /**
     * 只有读取的路径权限.
     */
    String ROLE_FRIEND = "FRIEND";
    String AUTHORITY_DIVIDE = "&&";

    Long UID_WHEN_NO_AUTH = 0L;

    // String LOGIN_PAGE_PATH = "/console/#/login";

    String[] SECURITY_MATCHER_PATHS = new String[]{
        "/api/**", "/apis/**", "/login", "/logout"
    };

    interface AnonymousUser {
        String PRINCIPAL = "anonymousUser";

        String Role = "anonymous";

        static boolean isAnonymousUser(String principal) {
            return PRINCIPAL.equals(principal);
        }
    }
}
