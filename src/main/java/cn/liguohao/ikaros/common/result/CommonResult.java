package cn.liguohao.ikaros.common.result;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.Strings;
import cn.liguohao.ikaros.common.TimeKit;
import java.io.Serializable;
import java.util.Objects;

/**
 * @author guohao
 * @date 2022/09/07
 */
public class CommonResult<T> extends BaseResult implements Serializable {
    private static final long serialVersionUID = -7268141541410717954L;

    private static final String DEFAULT_SUCCESS_MSG = "SUCCESS";
    private static final String DEFAULT_FAIL_MSG = "FAIL";

    public CommonResult() {

    }

    public CommonResult(boolean success, String message) {
        this.setSuccess(success);
        this.setMessage(message);
    }

    public CommonResult(boolean success) {
        this.setSuccess(success);
    }

    public CommonResult(String code, String message) {
        this.setCode(code);
        this.setMessage(message);
    }

    public CommonResult(boolean success, String message, T data) {
        this.setSuccess(success);
        this.setMessage(message);
        this.setData(data);
    }

    public CommonResult<T> setResult(T data) {
        this.setData(data);
        return this;
    }

    public T getData() {
        return (T) super.getData();
    }


    private static <T> CommonResult<T> baseCreate(String code, String msg, T date) {
        Assert.isNotBlank(code, msg);
        CommonResult<T> result = new CommonResult<T>();
        result.setCode(code);
        result.setSuccess(Objects.equals(code, ResultCode.SUCCESS));
        result.setMessage(msg);
        result.setData(date);
        result.setTimestamp(TimeKit.nowTimestamp());
        return result;
    }

    public static <T> CommonResult<T> ok(String code, String msg, T date) {
        if (code == null) {
            code = ResultCode.OTHER_ERR;
        }
        if (Strings.isBlank(msg)) {
            msg = DEFAULT_SUCCESS_MSG;
        }
        return baseCreate(code, msg, date);
    }

    public static <T> CommonResult<T> ok(String code, T date) {
        return ok(code, DEFAULT_SUCCESS_MSG, date);
    }

    public static <T> CommonResult<T> ok(T date) {
        return ok(ResultCode.SUCCESS, date);
    }

    public static <T> CommonResult<T> ok() {
        return ok(null);
    }


    public static <T> CommonResult<T> fail(String code, String msg, T date) {
        if (code == null) {
            code = ResultCode.OTHER_ERR;
        }
        if (Strings.isBlank(msg)) {
            msg = DEFAULT_FAIL_MSG;
        }
        return baseCreate(code, msg, date);
    }

    public static <T> CommonResult<T> fail(String code, T date) {
        return fail(code, DEFAULT_FAIL_MSG, date);
    }

    public static <T> CommonResult<T> fail(T date) {
        return fail(ResultCode.SUCCESS, date);
    }

    public static <T> CommonResult<T> fail(String msg) {
        return ok(ResultCode.OTHER_ERR, msg, null);
    }



}
