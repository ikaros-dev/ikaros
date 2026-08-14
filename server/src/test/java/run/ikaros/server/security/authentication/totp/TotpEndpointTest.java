package run.ikaros.server.security.authentication.totp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.reactive.function.server.ServerRequest;
import reactor.core.publisher.Mono;
import run.ikaros.server.security.authentication.jwt.JwtAuthenticationProvider;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.entity.UserTotpEntity;
import run.ikaros.server.store.repository.UserRepository;
import run.ikaros.server.store.repository.UserTotpRepository;

/**
 * TOTP 接口测试，验证禁用二步认证时的密码校验与配置删除行为.
 */
@org.jspecify.annotations.NullUnmarked
class TotpEndpointTest {
    /** TOTP 核心能力依赖. */
    @Mock
    private TotpService totpService;
    /** 用户 TOTP 配置仓库. */
    @Mock
    private UserTotpRepository userTotpRepository;
    /** JWT 认证能力依赖. */
    @Mock
    private JwtAuthenticationProvider jwtAuthenticationProvider;
    /** 当前用户详情查询服务. */
    @Mock
    private ReactiveUserDetailsService userDetailsService;
    /** 用户仓库. */
    @Mock
    private UserRepository userRepository;
    /** 当前密码校验器. */
    @Mock
    private PasswordEncoder passwordEncoder;
    /** 待测试的 TOTP 接口. */
    private TotpEndpoint endpoint;
    /** 已认证用户，用于写入响应式安全上下文. */
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        endpoint = new TotpEndpoint(totpService, userTotpRepository,
            jwtAuthenticationProvider, userDetailsService, userRepository,
            passwordEncoder);
        var userDetails = User.withUsername("tester")
            .password("encoded-password")
            .authorities("ROLE_USER")
            .build();
        authentication = new UsernamePasswordAuthenticationToken(
            userDetails, null, userDetails.getAuthorities());
        when(userDetailsService.findByUsername("tester"))
            .thenReturn(Mono.just(userDetails));
    }

    @Test
    void endpointReturnsRouterFunction() {
        assertThat(endpoint.endpoint()).isNotNull();
    }

    @Test
    void disableDeletesTotpWhenPasswordMatches() {
        var request = requestWithPassword("current-password");
        var userId = UUID.randomUUID();
        var totpId = UUID.randomUUID();
        var userEntity = mock(UserEntity.class);
        var totpEntity = mock(UserTotpEntity.class);
        when(passwordEncoder.matches("current-password", "encoded-password"))
            .thenReturn(true);
        when(userEntity.getId()).thenReturn(userId);
        when(userRepository.findByUsernameAndEnableAndDeleteStatus(
            "tester", true, false)).thenReturn(Mono.just(userEntity));
        when(userTotpRepository.findByUserId(userId))
            .thenReturn(Mono.just(totpEntity));
        when(totpEntity.getId()).thenReturn(totpId);
        when(userTotpRepository.deleteById(totpId)).thenReturn(Mono.empty());

        var response = endpoint.disable(request)
            .contextWrite(ReactiveSecurityContextHolder
                .withAuthentication(authentication))
            .block();

        assertThat(response).isNotNull();
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
        verify(userTotpRepository).deleteById(totpId);
    }

    @Test
    void disableRejectsIncorrectPassword() {
        var request = requestWithPassword("incorrect-password");
        when(passwordEncoder.matches("incorrect-password", "encoded-password"))
            .thenReturn(false);

        assertThatThrownBy(() -> endpoint.disable(request)
            .contextWrite(ReactiveSecurityContextHolder
                .withAuthentication(authentication))
            .block())
            .isInstanceOf(BadCredentialsException.class)
            .hasMessage("Invalid password");
        verifyNoInteractions(userTotpRepository);
    }

    private ServerRequest requestWithPassword(String password) {
        var request = mock(ServerRequest.class);
        when(request.bodyToMono(TotpDisableParam.class))
            .thenReturn(Mono.just(new TotpDisableParam().setPassword(password)));
        return request;
    }
}
