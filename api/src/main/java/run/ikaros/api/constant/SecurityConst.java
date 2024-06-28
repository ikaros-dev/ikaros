package run.ikaros.api.constant;

import static run.ikaros.api.constant.OpenApiConst.CORE_VERSION;

import org.springframework.http.HttpMethod;

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
    String AUTHORITY_DIVIDE = "&&&&&&";

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

    interface Authorization {
        interface Target {
            String ALL = "*";
            String API_CORE_USER = "/api/" + CORE_VERSION + "/user/**";
            String API_CORE_USERS = "/api/" + CORE_VERSION + "/users/**";
            String API_CORE_ROLE = "/api/" + CORE_VERSION + "/role/**";
            String API_CORE_ROLES = "/api/" + CORE_VERSION + "/roles/**";
            String API_CORE_STATIC = "/api/" + CORE_VERSION + "/static/**";
            String API_CORE_ATTACHMENT = "/api/" + CORE_VERSION + "/attachment/**";
            String API_CORE_ATTACHMENTS = "/api/" + CORE_VERSION + "/attachments/**";
            String API_CORE_SUBJECT = "/api/" + CORE_VERSION + "/subject/**";
            String API_CORE_SUBJECTS = "/api/" + CORE_VERSION + "/subjects/**";
            String API_CORE_TAG = "/api/" + CORE_VERSION + "/tag/**";
            String API_CORE_TAGS = "/api/" + CORE_VERSION + "/tags/**";
            String API_CORE_TASK = "/api/" + CORE_VERSION + "/task/**";
            String API_CORE_TASKS = "/api/" + CORE_VERSION + "/tasks/**";
            String API_CORE_PLUGIN = "/api/" + CORE_VERSION + "/plugin/**";
            String API_CORE_PLUGINS = "/api/" + CORE_VERSION + "/plugins/**";
            String API_CORE_INDICES = "/api/" + CORE_VERSION + "/indices/**";
            String API_CORE_EPISODE = "/api/" + CORE_VERSION + "/episode/**";
            String API_CORE_EPISODES = "/api/" + CORE_VERSION + "/episodes/**";
            String API_CORE_AUTHORITY = "/api/" + CORE_VERSION + "/authority/**";
            String API_CORE_AUTHORITIES = "/api/" + CORE_VERSION + "/authorities/**";
            String APIS_CUSTOM = "/apis/**";
            String MENU_DASHBOARD = "/dashboard/**";
            String MENU_ATTACHMENTS = "/attachments/**";
            String MENU_SUBJECTS = "/subjects/**";
            String MENU_PROFILE = "/profile/**";
            String MENU_COLLECTION = "/collection/**";
            String MENU_SETTING = "/setting/**";
            String MENU_ROLES = "/roles/**";
            String MENU_USERS = "/users/**";
            String MENU_PLUGINS = "/plugins/**";
            String MENU_ABOUT = "/about/**";
            String URL_CONSOLE = "/console/**";
            String URL_BASIC = "/**";
        }

        interface Authority {
            String ALL = "*";
            String HTTP_ALL = "HTTP_**";
            String HTTP_GET = "HTTP_" + HttpMethod.GET.name();
            String HTTP_HEAD = "HTTP_" + HttpMethod.HEAD.name();
            String HTTP_POST = "HTTP_" + HttpMethod.POST.name();
            String HTTP_PUT = "HTTP_" + HttpMethod.PUT.name();
            String HTTP_PATCH = "HTTP_" + HttpMethod.PATCH.name();
            String HTTP_DELETE = "HTTP_" + HttpMethod.DELETE.name();
            String HTTP_OPTIONS = "HTTP_" + HttpMethod.OPTIONS.name();
            String HTTP_TRACE = "HTTP_" + HttpMethod.TRACE.name();
        }
    }
}
