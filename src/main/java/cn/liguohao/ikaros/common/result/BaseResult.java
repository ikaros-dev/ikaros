package cn.liguohao.ikaros.common.result;

import java.io.Serializable;

/**
 * @author guohao
 * @date 2022/09/07
 */
public abstract class BaseResult<T> implements Serializable {
    private boolean success = false;

    private String message;

    private T data;

    private String code;

    private String timestamp;

    public boolean isSuccess() {
        return success;
    }

    public BaseResult<T> setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public BaseResult<T> setMessage(String message) {
        this.message = message;
        return this;
    }

    public T getData() {
        return data;
    }

    public BaseResult<T> setData(T data) {
        this.data = data;
        return this;
    }

    public String getCode() {
        return code;
    }

    public BaseResult<T> setCode(String code) {
        this.code = code;
        return this;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public BaseResult<T> setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }
}
