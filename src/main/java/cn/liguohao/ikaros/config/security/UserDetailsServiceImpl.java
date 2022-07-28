package cn.liguohao.ikaros.config.security;

import static cn.liguohao.ikaros.common.Assert.isNotBlank;

import cn.liguohao.ikaros.persistence.structural.entity.RoleEntity;
import cn.liguohao.ikaros.persistence.structural.entity.UserEntity;
import cn.liguohao.ikaros.service.UserService;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * @author li-guohao
 * @date 2022/3/26
 */
@Service
public record UserDetailsServiceImpl(UserService userService)
    implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        isNotBlank(username);
        // 查询用户
        UserEntity userEntity = userService.findByUsername(username);
        if (Objects.isNull(userEntity)) {
            throw new UsernameNotFoundException("user that username=" + username + " not found.");
        }
        // 构建UserDetails
        UserDetailsAdapter userDetailsAdapter = new UserDetailsAdapter(userEntity);

        // 填充权限
        Set<RoleEntity> roleEntities = userService.findRoleByUid(userEntity.getId());

        Set<GrantedAuthorityAdapter> authorityAdapters = roleEntities
            .stream()
            .flatMap((Function<RoleEntity, Stream<GrantedAuthorityAdapter>>) role
                -> Stream.of(new GrantedAuthorityAdapter(role)))
            .collect(Collectors.toSet());

        userDetailsAdapter.addAuthorities(authorityAdapters);
        return userDetailsAdapter;
    }
}
