package run.ikaros.api.infra.model;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.infra.utils.StringUtils;

/**
 * 所有的响应，HTTP状态都是200,具体的业务状态由下方结果的状态码显示.
 *
 * @param <T> 结果的数据体类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ResponseResult<T extends Object> {
    private Integer code;
    private String message;
    private List<T> body;
    private Throwable throwable;

    /**
     * Response success.
     */
    public static <T> ResponseResult<T> success(T... bodies) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.SUCCESS.getCode())
            .body(Arrays.asList(bodies))
            .message(ResponseCode.SUCCESS.name())
            .build();
    }

    /**
     * Response success.
     */
    public static <T> ResponseResult<T> success(List<T> bodies) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.SUCCESS.getCode())
            .body(bodies)
            .message(ResponseCode.SUCCESS.name())
            .build();
    }

    /**
     * Response not found.
     */
    public static <T> ResponseResult<T> notFound(String message, T... bodies) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.NOT_FOUND.getCode())
            .body(Arrays.asList(bodies))
            .message("[" + ResponseCode.NOT_FOUND.name() + "] " + message)
            .build();
    }

    /**
     * Response not found.
     */
    public static <T> ResponseResult<T> badRequest(String message, Throwable throwable) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.BAD_REQUEST.getCode())
            .message("[" + ResponseCode.BAD_REQUEST.name() + "] "
                + (StringUtils.isNotBlank(message) ? message
                : throwable.getMessage()))
            .build();
    }

    /**
     * Response not access.
     */
    public static <T> ResponseResult<T> notAccess(String message, Throwable throwable) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.NOT_ACCESS.getCode())
            //.throwable(throwable)
            .message("[" + ResponseCode.NOT_ACCESS.name() + "] "
                + (StringUtils.isNotBlank(message) ? message
                : throwable.getMessage()))
            .build();
    }

    /**
     * Response record exists.
     */
    public static <T> ResponseResult<T> recordExists(String message, Throwable throwable) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.RECORD_EXISTS.getCode())
            .message("[" + ResponseCode.RECORD_EXISTS.name() + "] "
                + (StringUtils.isNotBlank(message) ? message
                : throwable.getMessage()))
            .build();
    }

    /**
     * Response reference exists.
     */
    public static <T> ResponseResult<T> referenceExists(String message, Throwable throwable) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.REFERENCE_EXISTS.getCode())
            .throwable(throwable)
            .message("[" + ResponseCode.REFERENCE_EXISTS.name() + "] "
                + (StringUtils.isNotBlank(message) ? message
                : throwable.getMessage()))
            .build();
    }

    /**
     * Response unknown exception.
     */
    public static <T> ResponseResult<T> unknown(String message, Throwable throwable) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.UNKNOWN.getCode())
            .message("[" + ResponseCode.UNKNOWN.name() + "] "
                + (StringUtils.isNotBlank(message) ? message
                : throwable.getMessage()))
            .build();
    }

    /**
     * Response operate exception.
     */
    public static <T> ResponseResult<T> fail(String message, Throwable throwable) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.OPERATE_FAIL.getCode())
            .message("[" + ResponseCode.OPERATE_FAIL.name() + "] "
                + (StringUtils.isNotBlank(message) ? message
                : throwable.getMessage()))
            .build();
    }

}
