package cn.liguohao.ikaros.model.enums;

import cn.liguohao.ikaros.common.JacksonConverter;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author li-guohao
 */
public enum Role {

    /**
     * 主人
     */
    MASTER,

    /**
     * 访客
     */
    VISITOR,

    /**
     * 好友
     */
    FRIEND,

    /**
     * 关注的人
     */
    ATTRACTOR,

    /**
     * 关注我的人
     */
    FAN
    ;

    private static final Set<String> ROLE_NAME_SET =
        Arrays.stream(Role.values())
            .flatMap((Function<Role, Stream<String>>) role -> Stream.of(role.name()))
            .collect(Collectors.toSet());
    private static final String ROLE_NAMES = JacksonConverter.obj2Json(ROLE_NAME_SET);

    public static boolean contains(String roleName) {
        return ROLE_NAME_SET.contains(roleName);
    }

    public static String getRoleNames() {
        return ROLE_NAMES;
    }

}
