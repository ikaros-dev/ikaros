package run.ikaros.server.crd.router.define;

import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerResponse;
import run.ikaros.server.crd.router.define.ApiPathPatternGenerator;

/**
 * @author: li-guohao
 */
public interface CreateHandler extends HandlerFunction<ServerResponse>, ApiPathPatternGenerator {
}
