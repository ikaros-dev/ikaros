package run.ikaros.server.result;

/**
 * @author guohao
 * @date 2022/09/07
 */
public interface ResultCode {
    String SUCCESS = "200";
    String NOT_FOUND = "404";
    String UNAUTHORIZED = "401";
    String FORBIDDEN = "403";
    String OTHER_EXCEPTION = "500";
}
