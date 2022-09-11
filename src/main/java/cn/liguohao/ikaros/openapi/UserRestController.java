package cn.liguohao.ikaros.openapi;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.constants.SecurityConstants;
import cn.liguohao.ikaros.common.result.CommonResult;
import cn.liguohao.ikaros.model.AuthUser;
import cn.liguohao.ikaros.entity.UserEntity;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.service.UserService;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author guohao
 * @date 2022/09/08
 */
@RestController
@RequestMapping("/user")
public class UserRestController {

    private final UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public CommonResult<AuthUser> login(@RequestBody AuthUser authUser,
                                        @RequestHeader HttpHeaders httpHeaders)
        throws RecordNotFoundException {
        Assert.notNull(authUser, "'authUser' must not be null");
        authUser = userService.login(authUser);
        httpHeaders.set(SecurityConstants.TOKEN_HEADER,
            SecurityConstants.TOKEN_PREFIX + authUser.getToken());
        return CommonResult.ok(authUser);
    }

    //@PostMapping("/register")
    public CommonResult<UserEntity> register(@RequestBody AuthUser authUser) {
        Assert.notNull(authUser, "'authUser' must not be null");
        String username = authUser.getUsername();
        String password = authUser.getPassword();
        String role = authUser.getRole();
        userService.registerUserByUsernameAndPassword(username, password, role);
        return CommonResult.ok();
    }

    @GetMapping
    public CommonResult<List<UserEntity>> getUsers(@RequestBody UserEntity userEntityCondition) {
        // todo impl find users by condition
        return CommonResult.ok();
    }

    @GetMapping("/{id}")
    public CommonResult<UserEntity> getUserById(@PathVariable Long id)
        throws RecordNotFoundException {
        return CommonResult.ok(userService.findUserById(id));
    }

    @PutMapping
    public CommonResult<UserEntity> updateUser(@RequestBody UserEntity userEntity)
        throws RecordNotFoundException {
        return CommonResult.ok(userService.updateUserInfo(userEntity));
    }

    @DeleteMapping("/{id}")
    public CommonResult<String> deleteUserById(@PathVariable Long id)
        throws RecordNotFoundException {
        userService.deleteUserById(id);
        return CommonResult.ok();
    }

}
