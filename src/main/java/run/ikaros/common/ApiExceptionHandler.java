package run.ikaros.common;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 将领域异常收敛为稳定的 RFC 9457 问题响应。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 处理不可见或不存在的对象。
     *
     * @param exception 领域异常
     * @return 404 问题响应
     */
    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception.code(), exception.getMessage());
    }

    /**
     * 处理业务唯一性与状态冲突。
     *
     * @param exception 领域异常
     * @return 409 问题响应
     */
    @ExceptionHandler(ConflictException.class)
    public ProblemDetail handleConflict(ConflictException exception) {
        return problem(HttpStatus.CONFLICT, exception.code(), exception.getMessage());
    }

    @ExceptionHandler(StorageUnavailableException.class)
    public ProblemDetail handleStorageUnavailable(StorageUnavailableException exception) {
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "storage.unavailable", exception.getMessage());
    }

    @ExceptionHandler(InvalidRangeException.class)
    public ProblemDetail handleInvalidRange(InvalidRangeException exception) {
        return problem(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE, "range.invalid", exception.getMessage());
    }

    /**
     * 处理参数校验失败。
     *
     * @param exception 校验异常
     * @return 400 问题响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleValidation(ConstraintViolationException exception) {
        return problem(HttpStatus.BAD_REQUEST, "validation.failed", exception.getMessage());
    }

    /**
     * 处理服务层收到的非法参数。
     *
     * @param exception 参数异常
     * @return 400 问题响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "request.invalid", exception.getMessage());
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("code", code);
        return problem;
    }
}
