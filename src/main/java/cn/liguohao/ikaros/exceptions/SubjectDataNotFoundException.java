package cn.liguohao.ikaros.exceptions;

import cn.liguohao.ikaros.exceptions.IkarosException;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
public class SubjectDataNotFoundException extends IkarosException {
    public SubjectDataNotFoundException() {
    }

    public SubjectDataNotFoundException(String message) {
        super(message);
    }

    public SubjectDataNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubjectDataNotFoundException(Throwable cause) {
        super(cause);
    }

    public SubjectDataNotFoundException(String message, Throwable cause, boolean enableSuppression,
                                        boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
