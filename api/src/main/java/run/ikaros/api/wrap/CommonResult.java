package run.ikaros.api.wrap;

import lombok.Data;
import org.pf4j.PluginRuntimeException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.function.ServerResponse;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.AssertUtils;

@Data
public class CommonResult {
    private Integer code;
    private String message;
    private Throwable exception;

    /**
     * 根据异常类型动态生成不同状态码的结果.
     */
    public static CommonResult errorWithException(RuntimeException exception) {
        AssertUtils.notNull(exception, "'exception' must not null.");
        CommonResult cr = new CommonResult();
        cr.setMessage(exception.getMessage());
        cr.setException(exception);
        if (exception instanceof NotFoundException) {
            cr.setCode(HttpStatus.NOT_FOUND.value());
        } else if (exception instanceof AuthenticationException) {
            cr.setCode(HttpStatus.FORBIDDEN.value());
        } else if (exception instanceof PluginRuntimeException
                || exception instanceof IllegalArgumentException
                || exception instanceof DuplicateKeyException) {
            cr.setCode(HttpStatus.BAD_REQUEST.value());
        } else {
            cr.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
        return cr;
    }

    public static ServerResponse rspErrorWithException(RuntimeException exception) {
        CommonResult commonResult = errorWithException(exception);
        return ServerResponse.status(commonResult.getCode())
                .contentType(MediaType.APPLICATION_JSON).body(commonResult);
    }
}
