package cn.liguohao.ikaros.exceptions;

/**
 * 数据库未查到记录
 *
 * @author liguohao
 * @date 2022/07/30
 */
public class NotFoundRecordException extends IkarosRuntimeException {
    public NotFoundRecordException() {
    }

    public NotFoundRecordException(String message) {
        super(message);
    }

    public NotFoundRecordException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundRecordException(Throwable cause) {
        super(cause);
    }

    public NotFoundRecordException(String message, Throwable cause,
                                   boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
