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

    interface AnonymousUser {
        String PRINCIPAL = "anonymousUser";

        String Role = "anonymous";

        static boolean isAnonymousUser(String principal) {
            return PRINCIPAL.equals(principal);
        }
    }
}
