package run.ikaros.server.core.service;

import javax.annotation.Nonnull;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import run.ikaros.server.constants.SecurityConst;
import run.ikaros.server.constants.UserConst;
import run.ikaros.server.entity.UserEntity;
import run.ikaros.server.exceptions.RecordNotFoundException;
import run.ikaros.server.model.dto.AuthUserDTO;
import run.ikaros.server.model.dto.UserDTO;
import run.ikaros.server.utils.JwtUtils;
import run.ikaros.server.utils.StringUtils;

/**
 * @author li-guohao
 */
public interface UserService {
    @Nonnull
    @Transactional
    UserEntity registerUserByUsernameAndPassword(@Nonnull String username,
                                                 @Nonnull String password);

    /**
     * @return current login user id
     */
    static Long getCurrentLoginUserUid() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null
            && StringUtils.isNotBlank((String) authentication.getCredentials())) {
            String token = (String) authentication.getCredentials();
            return Long.valueOf(
                String.valueOf(JwtUtils.getTokenHeaderValue(token, SecurityConst.HEADER_UID)));
        }
        return UserConst.UID_WHEN_NO_AUTH;
    }

    @Nonnull
    AuthUserDTO login(@Nonnull AuthUserDTO authUserDTO) throws RecordNotFoundException;

    @Nonnull
    UserDTO getUserInfoByToken(@Nonnull String token);


}
