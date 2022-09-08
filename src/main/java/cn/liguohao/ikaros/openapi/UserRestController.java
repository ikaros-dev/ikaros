package cn.liguohao.ikaros.openapi;

import cn.liguohao.ikaros.UserDto;
import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.result.CommonResult;
import cn.liguohao.ikaros.entity.UserEntity;
import cn.liguohao.ikaros.exceptions.RecordNotFoundException;
import cn.liguohao.ikaros.service.UserService;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    @PostMapping
    public CommonResult<UserEntity> register(@RequestBody UserDto userDto) {
        Assert.notNull(userDto, "'userDto' must not be null");
        String username = userDto.getUsername();
        String password = userDto.getPassword();
        String role = userDto.getRole();
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
