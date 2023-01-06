package run.ikaros.server.infra.constant;

/**
 * application constants.
 *
 * @author: li-guohao
 */
public interface AppConst {
    String LOGIN_SUCCESS_LOCATION = "/console/";
    String LOGIN_FAILURE_LOCATION = "/console?error#/login";
    String LOGOUT_SUCCESS_LOCATION = "/console/?logout";
}
