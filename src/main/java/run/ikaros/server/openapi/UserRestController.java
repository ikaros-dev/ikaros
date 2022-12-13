package run.ikaros.server.openapi;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.constants.SecurityConst;
import run.ikaros.server.core.service.UserService;
import run.ikaros.server.core.service.UserSubscribeService;
import run.ikaros.server.entity.UserEntity;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.AuthUserDTO;
import run.ikaros.server.model.dto.UserDTO;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author guohao
 * @date 2022/09/08
 */
@RestController
@RequestMapping("/user")
public class UserRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRestController.class);

    private final UserService userService;
    private final UserSubscribeService userSubscribeService;

    public UserRestController(UserService userService, UserSubscribeService userSubscribeService) {
        this.userService = userService;
        this.userSubscribeService = userSubscribeService;
    }

    @PostMapping("/login")
    public CommonResult<AuthUserDTO> login(@RequestBody AuthUserDTO authUserDTO,
                                           @RequestHeader HttpHeaders httpHeaders)
        throws RecordNotFoundException {
        AssertUtils.notNull(authUserDTO, "'authUser' must not be null");
        LOGGER.debug("receive user info: {}", authUserDTO);
        authUserDTO = userService.login(authUserDTO);
        httpHeaders.set(SecurityConst.TOKEN_HEADER,
            SecurityConst.TOKEN_PREFIX + authUserDTO.getToken());
        return CommonResult.ok(authUserDTO);
    }

    private String getTokenFromHttpRequest(HttpServletRequest request) {
        String authorization = request.getHeader(SecurityConst.TOKEN_HEADER);
        if (authorization == null || !authorization.startsWith(SecurityConst.TOKEN_PREFIX)) {
            return null;
        }
        return authorization.replace(SecurityConst.TOKEN_PREFIX, "");
    }

    @GetMapping("/info")
    public CommonResult<UserDTO> getUserInfoByToken(HttpServletRequest request) {
        String token = getTokenFromHttpRequest(request);
        return CommonResult.ok(userService.getUserInfoByToken(token));
    }

    //@PostMapping("/register")
    public CommonResult<UserDTO> register(@RequestBody AuthUserDTO authUserDTO) {
        AssertUtils.notNull(authUserDTO, "'authUser' must not be null");
        String username = authUserDTO.getUsername();
        String password = authUserDTO.getPassword();
        UserEntity userEntity = userService.registerUserByUsernameAndPassword(username, password);
        return CommonResult.ok(new UserDTO(userEntity));
    }


    @GetMapping("/{id}")
    public CommonResult<UserDTO> getUserById(@PathVariable Long id)
        throws RecordNotFoundException {
        UserEntity userEntity = userService.getById(id);
        return CommonResult.ok(new UserDTO(userEntity));
    }

    @PutMapping
    public CommonResult<UserDTO> updateUser(@RequestBody UserEntity userEntity)
        throws RecordNotFoundException {
        return CommonResult.ok(new UserDTO(userService.updateUserInfo(userEntity)));
    }

    @DeleteMapping("/{id}")
    public CommonResult<String> deleteUserById(@PathVariable Long id)
        throws RecordNotFoundException {
        userService.deleteUserById(id);
        return CommonResult.ok();
    }

    @PutMapping("/subscribe/bgmtv/{subjectId}")
    public CommonResult<Boolean> saveUserSubscribeByBgmtvId(
        @PathVariable("subjectId") Long bgmtvSubjectId) {
        userSubscribeService.saveUserAnimeSubscribeByBgmTvSubjectId(
            UserService.getCurrentLoginUserUid(),
            bgmtvSubjectId);
        return CommonResult.ok(Boolean.TRUE);
    }

    @PutMapping("/subscribe/anime/{id}")
    public CommonResult<Boolean> saveUserSubscribeByAnimeId(
        @PathVariable("id") Long animeId,
        @RequestParam String progress,
        @RequestParam(required = false) String additional) {
        AssertUtils.notNull(animeId, "id");
        AssertUtils.notBlank(progress, "progress");

        if (StringUtils.isNotBlank(additional)) {
            additional =
                new String(Base64.getDecoder()
                    .decode(additional.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8);
            LOGGER.debug("additional: {}", additional);
        }
        userSubscribeService.saveUserAnimeSubscribe(
            UserService.getCurrentLoginUserUid(),
            animeId, progress, additional);
        return CommonResult.ok(Boolean.TRUE);
    }

    @PutMapping("/subscribe/anime/ids/{ids}")
    public CommonResult<Boolean> saveUserSubscribeWithBatchByAnimeIdArrBase64Json(
        @PathVariable("ids") String animeIdArrBase64Json
    ) {
        AssertUtils.notBlank(animeIdArrBase64Json, "animeIdArrBase64Json");
        byte[] bytes = Base64.getDecoder().decode(animeIdArrBase64Json);
        String animeIdArrJson = new String(bytes, StandardCharsets.UTF_8);
        Long[] animeIdArr = JsonUtils.json2ObjArr(animeIdArrJson, new TypeReference<Long[]>() {});
        userSubscribeService.saveUserSubscribeWithBatchByAnimeIdArr(animeIdArr);
        return CommonResult.ok(Boolean.TRUE);
    }

    @DeleteMapping("/subscribe/anime/{id}")
    public CommonResult<Boolean> deleteUserSubscribeByAnimeId(
        @PathVariable("id") Long animeId) {
        userSubscribeService.deleteUserAnimeSubscribe(
            UserService.getCurrentLoginUserUid(),
            animeId);
        return CommonResult.ok(Boolean.TRUE);
    }

    @PutMapping("/password")
    public CommonResult<UserEntity> updateUserPassword(
        @RequestParam("oldPassword") String oldPassword,
        @RequestParam("newPassword") String newPassword) {
        userService.updatePassword(UserService.getCurrentLoginUserUid(), oldPassword, newPassword);
        return CommonResult.ok();
    }
}
