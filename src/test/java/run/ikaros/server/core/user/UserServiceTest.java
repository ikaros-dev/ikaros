package run.ikaros.server.core.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Mono;
import run.ikaros.server.store.entity.UserEntity;

@SpringBootTest
class UserServiceTest {

    @Autowired
    UserService userService;

    @AfterEach
    void tearDown() {
        userService.deleteAll().block();
    }

    @Test
    void getUser() {
        String test1 = "test1";
        // create user
        UserEntity userEntity = UserEntity.builder()
            .username(test1)
            .password("old password")
            .build();
        userService.save(new User(userEntity)).block();

        // get user
        Mono<User> user = userService.getUser(test1);



    }

    @Test
    void updatePassword() {
    }
}