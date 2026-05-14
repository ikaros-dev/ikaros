package run.ikaros.server.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import run.ikaros.api.store.entity.User;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author chivehao
 * @since 2026-05-13
 */
public interface UserMapper extends BaseMapper<User> {
    @Select("select * from ikuser where username = #{username}")
    User findByUsername(String username);

    @Select("select * from ikuser where username = #{username} and enable = #{enable} and delete_status = #{deleteStatus}")
    User findByUsernameAndEnableAndDeleteStatus(String username, boolean enable, boolean deleteStatus);
}
