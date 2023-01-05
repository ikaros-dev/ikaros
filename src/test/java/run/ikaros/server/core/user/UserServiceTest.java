package run.ikaros.server.core.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
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
            .expectError(UserNotFoundException.class)
            .verify();
    }

    @Test
    void updatePassword() {
        final String username = "test1";
        final String oldPassword = "{bcrypt}old password";
        final String newPassword = "{bcrypt}new password";
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
            .flatMap(userEntity -> Mono.just(userEntity.getPassword()));

        StepVerifier.create(
                encodedPasswordMono
                    .flatMap(encodedPassword -> Mono.just(
                        passwordEncoder.matches(oldPassword, encodedPassword)))
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
    }
}