package cn.liguohao.ikaros.exceptions;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
public class SubjectDataDeleteFailException extends IkarosException {
    public SubjectDataDeleteFailException() {
    }

    public SubjectDataDeleteFailException(String message) {
        super(message);
    }

    public SubjectDataDeleteFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubjectDataDeleteFailException(Throwable cause) {
        super(cause);
    }

    public SubjectDataDeleteFailException(String message, Throwable cause,
                                          boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
