package run.ikaros.api.wrap;

import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.pf4j.PluginRuntimeException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.function.ServerResponse;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.infra.utils.AssertUtils;

@Slf4j
@Data
public class CommonResult {

    private int httpCode;
    private String httpStatus;
    private String customMessage;
    private String hintMessage;
    private String exceptionMessage;
    private Throwable exception;
    private String requestId;
    private String requestUri;

    public CommonResult setHttpStatus(HttpStatus httpStatus) {
        this.httpCode = httpStatus.value();
        this.httpStatus = httpStatus.name();
        return this;
    }

    /**
     * 根据异常类型动态生成不同状态码的结果.
     */
    public static CommonResult errorWithException(RuntimeException exception) {
        AssertUtils.notNull(exception, "'exception' must not null.");
        CommonResult cr = new CommonResult();
        cr.setExceptionMessage(exception.getLocalizedMessage());
        // cr.setException(exception); 堆栈信息有点多，最多只在日志里打个告警
        //log.warn("Server response has exception: {}", cr.getExceptionMessage());
        if (exception instanceof NotFoundException) {
            cr.setHttpStatus(HttpStatus.NOT_FOUND);
            cr.setHintMessage("大概率是因为资源不存在！");
        } else if (exception instanceof AuthenticationException) {
            cr.setHttpStatus(HttpStatus.UNAUTHORIZED);
            cr.setHintMessage("未认证，请先认证再访问资源！");
        }else if (exception instanceof AccessDeniedException) {
            cr.setHttpStatus(HttpStatus.FORBIDDEN);
            cr.setHintMessage("禁止访问目标资源，已认证但是没有对应资源的访问权限，前面的区域后面再来探索吧！");
        } else if (exception instanceof PluginRuntimeException
                || exception instanceof IllegalArgumentException
                || exception instanceof DuplicateKeyException) {
            cr.setHttpStatus(HttpStatus.BAD_REQUEST);
        } else {
            cr.setHintMessage("服务端内部出现了未知的错误！请联系管理人员处理。");
            cr.setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return cr;
    }

}
