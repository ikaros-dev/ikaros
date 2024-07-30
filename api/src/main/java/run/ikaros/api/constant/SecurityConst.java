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
            String API_PREFIX = "/api/" + CORE_VERSION;
            String API_CORE_USER = API_PREFIX + "/user/**";
            String API_CORE_USERS = API_PREFIX + "/users/**";
            String API_CORE_USER_ROLE = API_PREFIX + "/user/role/**";
            String API_CORE_USER_ROLES = API_PREFIX + "/user/roles/**";
            String API_CORE_ROLE = API_PREFIX + "/role/**";
            String API_CORE_ROLES = API_PREFIX + "/roles/**";
            String API_CORE_ROLE_AUTHORITY = API_PREFIX + "/role/authority/**";
            String API_CORE_ROLE_AUTHORITIES = API_PREFIX + "/role/authorities/**";
            String API_CORE_STATIC = API_PREFIX + "/static/**";
            String API_CORE_ATTACHMENT = API_PREFIX + "/attachment/**";
            String API_CORE_ATTACHMENTS = API_PREFIX + "/attachments/**";
            String API_CORE_ATTACHMENT_REFERENCE = API_PREFIX + "/attachment/reference/**";
            String API_CORE_ATTACHMENT_REFERENCES = API_PREFIX + "/attachment/references/**";
            String API_CORE_ATTACHMENT_RELATION = API_PREFIX + "/attachment/relation/**";
            String API_CORE_ATTACHMENT_RELATIONS = API_PREFIX + "/attachment/relations/**";
            String API_CORE_SUBJECT = API_PREFIX + "/subject/**";
            String API_CORE_SUBJECTS = API_PREFIX + "/subjects/**";
            String API_CORE_SUBJECT_RELATION = API_PREFIX + "/subject/relation/**";
            String API_CORE_SUBJECT_RELATIONS = API_PREFIX + "/subject/relations/**";
            String API_CORE_SUBJECT_SYNC_PLATFORM = API_PREFIX + "/subject/sync/platform/**";
            String API_CORE_SUBJECT_SYNC_PLATFORMS = API_PREFIX + "/subject/sync/platforms/**";
            String API_CORE_TAG = API_PREFIX + "/tag/**";
            String API_CORE_TAGS = API_PREFIX + "/tags/**";
            String API_CORE_TASK = API_PREFIX + "/task/**";
            String API_CORE_TASKS = API_PREFIX + "/tasks/**";
            String API_CORE_PLUGIN = API_PREFIX + "/plugin/**";
            String API_CORE_PLUGINS = API_PREFIX + "/plugins/**";
            String API_CORE_INDICES = API_PREFIX + "/indices/**";
            String API_CORE_EPISODE = API_PREFIX + "/episode/**";
            String API_CORE_EPISODES = API_PREFIX + "/episodes/**";
            String API_CORE_AUTHORITY = API_PREFIX + "/authority/**";
            String API_CORE_AUTHORITIES = API_PREFIX + "/authorities/**";
            String API_CORE_COLLECTION = API_PREFIX + "/collection/**";
            String API_CORE_COLLECTIONS = API_PREFIX + "/collections/**";
            String API_CORE_COLLECTION_EPISODE = API_PREFIX + "/collection/episode/**";
            String API_CORE_COLLECTION_EPISODES = API_PREFIX + "/collection/episodes/**";
            String API_CORE_COLLECTION_SUBJECT = API_PREFIX + "/collection/subject/**";
            String API_CORE_COLLECTION_SUBJECTS = API_PREFIX + "/collection/subjects/**";
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
