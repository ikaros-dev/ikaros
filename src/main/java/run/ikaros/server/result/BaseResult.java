package run.ikaros.server.result;

import java.io.Serializable;

/**
 * @author guohao
 * @date 2022/09/07
 */
public abstract class BaseResult<T> implements Serializable {
    private boolean success = false;

    private String message;

    private T result;

    private String code;

    private String timestamp;

    private Throwable throwable;

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

    public T getResult() {
        return result;
    }

    public BaseResult<T> setResult(T result) {
        this.result = result;
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

    public Throwable getThrowable() {
        return throwable;
    }

    public BaseResult<T> setThrowable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }
}
