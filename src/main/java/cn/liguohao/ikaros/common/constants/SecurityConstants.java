package cn.liguohao.ikaros.common.constants;

/**
 * @author guohao
 * @date 2022/09/11
 */
public interface SecurityConstants {

    String API_AUTH_LOGIN_URL = "/api/user/login";
    String API_AUTH_LOGOUT_URL = "/auth/user/logout";
    String API_USER_REGISTER_URL = "/api/user/register";
    String[] API_STATUS_URLS = {
        "/api/status/ping",
        "/api/status/hello",
        "/api/status/running"
    };


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
