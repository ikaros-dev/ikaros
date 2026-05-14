package run.ikaros.server.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import run.ikaros.api.store.entity.User;

public interface UserMapper extends BaseMapper<User> {
    @Select("select * from ikuser where username = #{username}")
    User findByUsername(String username);

    @Select("select * from ikuser "
        + "where username = #{username} and enable = #{enable} and delete_status = #{deleteStatus}")
    User findByUsernameAndEnableAndDeleteStatus(String username, boolean enable,
                                                boolean deleteStatus);
}
