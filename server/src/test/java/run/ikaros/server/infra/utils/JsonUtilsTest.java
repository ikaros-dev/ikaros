package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.ArrayList;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {


    @Test
    void json2ObjArr() {
        List<User> userList = new ArrayList<>();
        userList.add(new User("u1", "p1"));
        userList.add(new User("u2", "p2"));
        userList.add(new User("u3", "p3"));

        String json = JsonUtils.obj2Json(userList);

        TypeReference<User[]> userTypeReference = new TypeReference<User[]>() {
        };
        User[] users = JsonUtils.json2ObjArr(json, userTypeReference);
        assertNotNull(users);
        assertEquals(users.length, userList.size());

    }


    @Test
    void objAndJson() {
        String json = JsonUtils.obj2Json(new User().setUsername("u1"));
        Assertions.assertThat(json).isNotBlank();

        User user1 = JsonUtils.json2obj(json, User.class);
        Assertions.assertThat(user1).isNotNull();
        Assertions.assertThat(user1.getUsername()).isEqualTo("u1");
    }


    static class User {
        private String username;
        private String password;

        public User() {
        }

        public User(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() {
            return username;
        }

        public User setUsername(String username) {
            this.username = username;
            return this;
        }

        public String getPassword() {
            return password;
        }

        public User setPassword(String password) {
            this.password = password;
            return this;
        }
    }

}