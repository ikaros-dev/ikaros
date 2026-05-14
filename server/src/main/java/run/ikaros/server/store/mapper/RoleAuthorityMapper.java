package run.ikaros.server.store.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import run.ikaros.api.store.entity.RoleAuthority;

import java.util.List;
import java.util.UUID;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author chivehao
 * @since 2026-05-13
 */
public interface RoleAuthorityMapper extends BaseMapper<RoleAuthority> {
    @Select("select * from role_authority where role_id = #{roleId}")
    List<RoleAuthority> findAllByRoleId(UUID roleId);
}
