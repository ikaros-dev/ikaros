package run.ikaros.server.infra.constant;

/**
 * security constants.
 *
 * @author: li-guohao
 */
public interface SecurityConst {
    String DEFAULT_ROLE = "MASTER";

    interface AnonymousUser {
        String PRINCIPAL = "anonymousUser";

        String Role = "anonymous";

        static boolean isAnonymousUser(String principal) {
            return PRINCIPAL.equals(principal);
        }
    }
}
