package run.ikaros.server.constants;

/**
 * @author guohao
 * @date 2022/09/11
 */
public interface SecurityConst {

    String API_AUTH_LOGIN_URL = "/api/user/login";
    String API_AUTH_LOGOUT_URL = "/api/user/logout";
    String API_AUTH_OPTION_IS_INIT = "/api/option/app/is-init";
    String API_AUTH_OPTION_APP_INIT = "/api/option/app/init";
    String API_USER_REGISTER_URL = "/api/user/register";
    String[] PAGE_ADMIN_URL = {
        "/admin/**", "/js/**", "/css/**", "/img/**", "/logo.png"
    };
    String[] API_STATUS_URLS = {
        "/api/status/ping",
        "/api/status/hello",
        "/api/status/running"
    };

    String[] SWAGGER_DOC_URLS = {
        "/v2/api-docs/**",
        "/v3/api-docs/**",
        "/i18n/**",
        "/webjars/springfox-swagger-ui/**",
        "/swagger-resources/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/swagger-ui"
    };

    String[] APP_URLS = {
        "/app/**/*.{js,html}",
        "/h2",
    };

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

    String HIDDEN_STR = "***hidden***";
}
