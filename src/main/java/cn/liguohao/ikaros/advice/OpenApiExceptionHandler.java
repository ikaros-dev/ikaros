package cn.liguohao.ikaros.advice;

import cn.liguohao.ikaros.common.result.CommonResult;
import cn.liguohao.ikaros.common.result.ResultCode;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @author guohao
 * @date 2022/09/08
 */
@RestControllerAdvice
public class OpenApiExceptionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenApiExceptionHandler.class);

    @ExceptionHandler(value = RecordNotFoundException.class)
    public CommonResult<String> recordNotFoundException(RecordNotFoundException exception) {
        LOGGER.error(exception.getClass().getSimpleName() + ": ", exception);
        final String msg = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        return CommonResult.fail(ResultCode.NOT_FOUND, msg,
            null, reduceStackTraceLength2Three(exception));
    }


    @ExceptionHandler(value = Exception.class)
    public CommonResult<String> exception(Exception exception) {
        LOGGER.error(exception.getClass().getSimpleName() + ": ", exception);
        final String msg = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        return CommonResult.fail(ResultCode.OTHER_EXCEPTION, msg,
            null, reduceStackTraceLength2Three(exception));
    }

    private Exception reduceStackTraceLength2Three(Exception exception) {
        StackTraceElement[] stackTrace = exception.getStackTrace();
        StackTraceElement[] newStackTrace = new StackTraceElement[3];
        newStackTrace[0] = stackTrace[0];
        newStackTrace[1] = stackTrace[1];
        newStackTrace[2] = stackTrace[2];
        exception.setStackTrace(newStackTrace);
        return exception;
    }

}
