package run.ikaros.server.core.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.server.infra.exception.NotFoundException;
import run.ikaros.server.store.entity.UserEntity;

@SpringBootTest
class UserServiceTest {

    @Autowired
    UserService userService;
    @Autowired
    PasswordEncoder passwordEncoder;

    @AfterEach
    void tearDown() {
        userService.deleteAll().block();
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
            .block();


        // verify get user
        StepVerifier.create(
                userService.getUser(test1)
                    .map(User::entity)
                    .flatMap(userEntity -> Mono.just(userEntity.getUsername())))
            .expectNext(test1)
            .verifyComplete();

        // verify throw UserNotFoundException when get not exists user
        StepVerifier.create(userService.getUser("not-exists-user"))
            .expectError(NotFoundException.class)
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
            .block();

        Mono<String> encodedPasswordMono = userService.getUser(username)
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
        userService.updatePassword(username, newPassword).block();

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
        userService.updatePassword(username, newPassword).block();
        StepVerifier.create(
                encodedPasswordMono
                    .flatMap(encodedPassword -> Mono.just(
                        passwordEncoder.matches(newPassword, encodedPassword)
                    ))
            ).expectNext(Boolean.TRUE)
            .verifyComplete();

        // update by not same password
        final String newPassword2 = "newPassword2";
        userService.updatePassword(username, newPassword2).block();
        StepVerifier.create(
                encodedPasswordMono
                    .flatMap(encodedPassword -> Mono.just(
                        passwordEncoder.matches(newPassword2, encodedPassword)
                    ))
            ).expectNext(Boolean.TRUE)
            .verifyComplete();
    }
}