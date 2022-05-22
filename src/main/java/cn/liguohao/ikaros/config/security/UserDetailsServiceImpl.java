package cn.liguohao.ikaros.config.security;

import static cn.liguohao.ikaros.common.Assert.isNotBlank;

import cn.liguohao.ikaros.entity.Role;
import cn.liguohao.ikaros.entity.User;
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
        User user = userService.findByUsername(username);
        if (Objects.isNull(user)) {
            throw new UsernameNotFoundException("user that username=" + username + " not found.");
        }
        // 构建UserDetails
        UserDetailsAdapter userDetailsAdapter = new UserDetailsAdapter(user);

        // 填充权限
        Set<Role> roles = userService.findRoleByUid(user.getId());

        Set<GrantedAuthorityAdapter> authorityAdapters = roles
            .stream()
            .flatMap((Function<Role, Stream<GrantedAuthorityAdapter>>) role
                -> Stream.of(new GrantedAuthorityAdapter(role)))
            .collect(Collectors.toSet());

        userDetailsAdapter.addAuthorities(authorityAdapters);
        return userDetailsAdapter;
    }
}
