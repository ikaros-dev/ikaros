package run.ikaros.api.wrap;

import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

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
     * Response record exists.
     */
    public static <T> ResponseResult<T> recordExists(String message, Throwable throwable) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.RECORD_EXISTS.getCode())
            .message("[" + ResponseCode.RECORD_EXISTS.name() + "] " + message)
            .throwable(throwable)
            .build();
    }

    /**
     * Response reference exists.
     */
    public static <T> ResponseResult<T> referenceExists(String message, Throwable throwable) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.REFERENCE_EXISTS.getCode())
            .message("[" + ResponseCode.REFERENCE_EXISTS.name() + "] " + message)
            .throwable(throwable)
            .build();
    }

    /**
     * Response unknown exception.
     */
    public static <T> ResponseResult<T> unknown(String message, Throwable throwable) {
        return ResponseResult.<T>builder()
            .code(ResponseCode.UNKNOWN.getCode())
            .message("[" + ResponseCode.UNKNOWN.name() + "] " + message)
            .throwable(throwable)
            .build();
    }

}
