package run.ikaros.server.infra.constant;

/**
 * security constants.
 *
 * @author: li-guohao
 */
public interface SecurityConst {
    String ROLE_MASTER = "MASTER";

    interface AnonymousUser {
        String PRINCIPAL = "anonymousUser";

        String Role = "anonymous";

        static boolean isAnonymousUser(String principal) {
            return PRINCIPAL.equals(principal);
        }
    }
}
