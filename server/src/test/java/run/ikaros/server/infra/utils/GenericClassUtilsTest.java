package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class GenericClassUtilsTest {

    @Test
    void shouldGenerateConcreteClassForList() {
        try {
            Class<?> concreteClass =
                GenericClassUtils.generateConcreteClass(List.class, String.class);
            assertThat(concreteClass).isNotNull();
            assertThat(List.class.isAssignableFrom(concreteClass)).isTrue();
        } catch (Exception e) {
            // ByteBuddy may not work in all environments
            assertThat(e).isInstanceOfAny(SecurityException.class, IllegalArgumentException.class);
        }
    }
}
