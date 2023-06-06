package run.ikaros.api.constant;

/**
 * security constants.
 *
 * @author: li-guohao
 */
public interface SecurityConst {
    String PREFIX = "ROLE_";
    String ROLE_MASTER = "MASTER";

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
