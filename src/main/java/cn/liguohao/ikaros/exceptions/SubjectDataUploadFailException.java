package cn.liguohao.ikaros.exceptions;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
public class SubjectDataUploadFailException extends IkarosException {
    public SubjectDataUploadFailException() {
    }

    public SubjectDataUploadFailException(String message) {
        super(message);
    }

    public SubjectDataUploadFailException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubjectDataUploadFailException(Throwable cause) {
        super(cause);
    }

    public SubjectDataUploadFailException(String message, Throwable cause,
                                          boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
