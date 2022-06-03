package cn.liguohao.ikaros.common.exceptions.service;

import cn.liguohao.ikaros.common.exceptions.IkarosRuntimeException;

/**
 * 查无此关系
 *
 * @author li-guohao
 * @date 2022/06/03
 */
public class RelationNotExistException extends IkarosRuntimeException {
    public RelationNotExistException() {
    }

    public RelationNotExistException(String message) {
        super(message);
    }

    public RelationNotExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public RelationNotExistException(Throwable cause) {
        super(cause);
    }

    public RelationNotExistException(String message, Throwable cause, boolean enableSuppression,
                                     boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
