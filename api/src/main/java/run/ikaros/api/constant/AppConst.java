package run.ikaros.api.constant;

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
    String CACHE_DIR_NAME = "caches";

    /**
     * currentTime / TotalTime .
     */
    Double EPISODE_FINISH = 0.9375 * 0.85;
}
