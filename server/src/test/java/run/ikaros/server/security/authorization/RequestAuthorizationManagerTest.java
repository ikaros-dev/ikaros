package run.ikaros.server.security.authorization;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;

import java.util.Random;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;
import run.ikaros.api.constant.SecurityConst;
import run.ikaros.server.core.user.RoleService;
import run.ikaros.server.core.user.User;
import run.ikaros.server.core.user.UserService;
import run.ikaros.server.security.SecurityProperties;
import run.ikaros.server.store.entity.RoleEntity;
import run.ikaros.server.store.entity.UserEntity;
import run.ikaros.server.store.repository.AuthorityRepository;
import run.ikaros.server.store.repository.RoleRepository;

@SpringBootTest
@AutoConfigureWebTestClient
class RequestAuthorizationManagerTest {

    @Autowired
    private UserService userService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private AuthorityRepository authorityRepository;

    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    WebTestClient webTestClient;
    @Autowired
    SecurityProperties securityProperties;


    private String username;
    private String password;

    @BeforeEach
    void setUp() {
        webTestClient = webTestClient.mutateWith(csrf());
        username = securityProperties.getInitializer().getMasterUsername();
        password = securityProperties.getInitializer().getMasterPassword();
    }

    @AfterEach
    void tearDown() {
        webTestClient = webTestClient.mutateWith(csrf());
        username = securityProperties.getInitializer().getMasterUsername();
        password = securityProperties.getInitializer().getMasterPassword();
        StepVerifier.create(userService.deleteAll()).verifyComplete();
        StepVerifier.create(roleRepository.deleteAll()).verifyComplete();
        StepVerifier.create(authorityRepository.deleteAll()).verifyComplete();
    }

    @Test
    void checkRoleFriend() {
        Random random = new Random();
        var username = String.valueOf(random.nextInt(1, 100));

        UserEntity friend = new UserEntity();
        friend.setUsername(username);
        friend.setPassword(password);
        StepVerifier.create(userService.save(new User(friend))
                .map(User::entity)
                .map(UserEntity::getUsername))
            .expectNext(username)
            .verifyComplete();

        StepVerifier.create(roleService.createIfNotExist(SecurityConst.ROLE_FRIEND)
                .map(RoleEntity::getId)
                .map(friend::setRoleId)
                .flatMap(f -> userService.save(new User(f)))
                .map(User::entity)
                .map(UserEntity::getUsername))
            .expectNext(username)
            .verifyComplete();

        StepVerifier.create(authorityRepository.count())
            .expectNextMatches(count -> count >= 2)
            .verifyComplete();

    }
}