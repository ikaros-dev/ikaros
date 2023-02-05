package run.ikaros.server.infra.constant;

import java.time.Duration;

/**
 * application constants.
 *
 * @author: li-guohao
 */
public interface AppConst {
    String LOGIN_SUCCESS_LOCATION = "/console/";
    String LOGIN_FAILURE_LOCATION = "/console/?error#/login";
    String LOGOUT_SUCCESS_LOCATION = "/console/?logout";
    Duration BLOCK_TIMEOUT = Duration.ofMillis(2000L);
}
