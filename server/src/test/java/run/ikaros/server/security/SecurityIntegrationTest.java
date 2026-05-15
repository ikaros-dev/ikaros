package run.ikaros.server.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(
    properties = {
        "ikaros.security.initializer.disabled=true",
        "spring.flyway.enabled=true",
        "ikaros.workDir=target/test-ikaros",
        "ikaros.externalUrl=http://localhost:9999"
    }
)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("ikaros_test")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @RestController
    static class StubController {
        @GetMapping("/api/v1/user/me")
        public String me() {
            return "me";
        }

        @GetMapping("/api/v1/subject/list")
        public String subjects() {
            return "subjects";
        }

        @GetMapping("/api/v1/static/test.css")
        public String staticResource() {
            return "body { color: black; }";
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unauthenticatedRequest_toProtectedResource_shouldBeDenied() throws Exception {
        mockMvc.perform(get("/api/v1/user/me"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unauthenticatedRequest_toStaticResource_shouldBeGranted() throws Exception {
        mockMvc.perform(get("/api/v1/static/test.css"))
            .andExpect(status().isOk());
    }
}
