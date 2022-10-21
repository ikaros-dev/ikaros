package cn.liguohao.ikaros.model.binary;

import cn.liguohao.ikaros.common.Assert;

/**
 * @author guohao
 * @date 2022/10/21
 */
public interface BinaryStorge {
    Binary add(Binary binary);

    void delete(Binary binary);

    default void delete(String url) {
        Assert.notBlank(url, "'url' must not be empty");
        delete(new Binary().setUrl(url));
    }

    boolean exists(Binary binary);

    default boolean exists(String url) {
        Assert.notBlank(url, "'url' must not be empty");
        return exists(new Binary().setUrl(url));
    }

    BinaryPlace getPlace(Binary binary);
}
