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

    String API_CORE_MATCHER = "/api/**";
    String API_PLUGIN_MATCHER = "/apis/**";

    // todo 安全性考虑，这里需要从环境变量读取，或者在 初始化时在数据库随机生成一条
    String JWT_SECRET_KEY = "v8y/B?E(G+KbPeShVmYq3t6w9z$C&F)J@McQfTjWnZr4u7x!A%D*G-KaPdRgUkXp";
    String TOKEN_PREFIX = "Bearer ";
    String TOKEN_HEADER = "Authorization";
    String TOKEN_TYPE = "JWT";
    String TOKEN_ROLE_CLAIM = "role";
    String TOKEN_ISSUER = "security";
    String TOKEN_AUDIENCE = "security-all";
    long EXPIRATION_TIME = 60 * 60 * 2L;
    long EXPIRATION_REMEMBER_TIME = 60 * 60 * 24 * 7L;
    String HEADER_UID = "UID";
}
