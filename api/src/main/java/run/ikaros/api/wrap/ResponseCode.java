package run.ikaros.api.wrap;

import lombok.Getter;

@Getter
public enum ResponseCode {
    // 一些统一的状态，沿用HTTP状态码相同含义。
    /**
     * 一切正常.
     */
    SUCCESS(200),
    /**
     * 请求参数有问题.
     */
    BAD_REQUEST(400),
    /**
     * 数据没有找到.
     */
    NOT_FOUND(404),
    /**
     * 其它未知的异常.
     */
    UNKNOWN(500),

    // 业务的状态码 五位数，其中第三位和 HTTP状态码第一位含义类似
    /**
     * 数据已经存在.
     */
    RECORD_EXISTS(10401),
    /**
     * 数据的引用存在.
     */
    REFERENCE_EXISTS(10402),

    ;
    private final int code;

    ResponseCode(int code) {
        this.code = code;
    }
}
