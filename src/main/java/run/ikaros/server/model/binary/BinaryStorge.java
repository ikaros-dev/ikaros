package run.ikaros.server.model.binary;

import run.ikaros.server.utils.AssertUtils;

/**
 * @author guohao
 * @date 2022/10/21
 */
public interface BinaryStorge {
    Binary add(Binary binary);

    void delete(Binary binary);

    default void delete(String url) {
        AssertUtils.notBlank(url, "'url' must not be empty");
        delete(new Binary().setUrl(url));
    }

    boolean exists(Binary binary);

    default boolean exists(String url) {
        AssertUtils.notBlank(url, "'url' must not be empty");
        return exists(new Binary().setUrl(url));
    }

    BinaryPlace getPlace(Binary binary);
}
