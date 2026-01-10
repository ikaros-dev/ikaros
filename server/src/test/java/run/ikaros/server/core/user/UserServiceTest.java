package run.ikaros.server.core.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static run.ikaros.api.constant.AppConst.BLOCK_TIMEOUT;
import static run.ikaros.server.core.user.UserService.DEFAULT_PASSWORD_ENCODING_ID_PREFIX;
import static run.ikaros.server.test.TestConst.PROCESS_SHOULD_NOT_RUN_TO_THIS;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.infra.exception.security.PasswordNotMatchingException;
import run.ikaros.server.security.SecurityProperties;
import run.ikaros.server.store.entity.UserEntity;

@SpringBootTest
@AutoConfigureWebTestClient
class UserServiceTest {

    @Autowired
    UserService userService;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    WebTestClient webClient;
    @Autowired
    SecurityProperties securityProperties;
    String username;
    String password;

    @BeforeEach
    void setUp() {
        webClient = webClient.mutateWith(csrf());
        username = securityProperties.getInitializer().getMasterUsername();
        password = securityProperties.getInitializer().getMasterPassword();
    }

    @AfterEach
    void tearDown() {
        userService.deleteAll().block(BLOCK_TIMEOUT);
    }

    @Test
    void addEncodingIdPrefixIfNotExists() {
        try {
            UserService.addEncodingIdPrefixIfNotExists("");
            fail(PROCESS_SHOULD_NOT_RUN_TO_THIS);
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).isEqualTo("'rawPassword' must has text");
        }

        String password = "password";

        assertThat(UserService.addEncodingIdPrefixIfNotExists(password))
            .isEqualTo(DEFAULT_PASSWORD_ENCODING_ID_PREFIX + password);
        assertThat(UserService.addEncodingIdPrefixIfNotExists(
            DEFAULT_PASSWORD_ENCODING_ID_PREFIX + password))
            .isEqualTo(DEFAULT_PASSWORD_ENCODING_ID_PREFIX + password);
    }

    @Test
    void getUser() {
        final String test1 = "test1";
        // create user
        Mono.just(UserEntity.builder()
                .username(test1)
                .password("old password")
                .build())
            .map(User::new)
            .flatMap(userService::save)
            .block(BLOCK_TIMEOUT);


        // verify get user
        StepVerifier.create(
                userService.getUserByUsername(test1)
                    .map(User::entity)
                    .flatMap(userEntity -> Mono.just(userEntity.getUsername())))
            .expectNext(test1)
            .verifyComplete();

        // verify throw UsernameNotFoundException when get not exists user
        StepVerifier.create(userService.getUserByUsername("not-exists-user"))
            .expectError(UsernameNotFoundException.class)
            .verify();
    }

    @Test
    void updatePassword() {
        final String username = "test1";
        final String oldPassword = "old password";
        final String newPassword = "new password";
        // create user
        Mono.just(UserEntity.builder()
                .username(username)
                .password(oldPassword)
                .build())
            .map(User::new)
            .flatMap(userService::save)
            .block(BLOCK_TIMEOUT);

        Mono<String> encodedPasswordMono = userService.getUserByUsername(username)
            .map(User::entity)
            .flatMap(userEntity -> Mono.just(userEntity.getPassword()))
            .map(UserService::addEncodingIdPrefixIfNotExists);

        StepVerifier.create(
                encodedPasswordMono
                    .flatMap(encodedPassword -> Mono.just(
                        passwordEncoder.matches(oldPassword,
                            encodedPassword)))
            ).expectNext(Boolean.TRUE)
            .verifyComplete();

        // update user password
        userService.updatePassword(username, oldPassword, newPassword).block(BLOCK_TIMEOUT);

        StepVerifier.create(
                encodedPasswordMono
                    .flatMap(encodedPassword -> Mono.just(
                        passwordEncoder.matches(oldPassword, encodedPassword)
                    ))
            ).expectNext(Boolean.FALSE)
            .verifyComplete();
        StepVerifier.create(
                encodedPasswordMono
                    .flatMap(encodedPassword -> Mono.just(
                        passwordEncoder.matches(newPassword, encodedPassword)
                    ))
            ).expectNext(Boolean.TRUE)
            .verifyComplete();

        // update by same password
        StepVerifier.create(userService.updatePassword(username, oldPassword, newPassword))
            .expectErrorMatches(throwable ->
                throwable.getClass() == PasswordNotMatchingException.class
                    && throwable.getMessage().startsWith("Old password not matching username: "))
            .verify();


        // update by not same password
        final String newPassword2 = "newPassword2";
        userService.updatePassword(username, newPassword, newPassword2).block(BLOCK_TIMEOUT);
        StepVerifier.create(
                encodedPasswordMono
                    .flatMap(encodedPassword -> Mono.just(
                        passwordEncoder.matches(newPassword2, encodedPassword)
                    ))
            ).expectNext(Boolean.TRUE)
            .verifyComplete();
    }


}