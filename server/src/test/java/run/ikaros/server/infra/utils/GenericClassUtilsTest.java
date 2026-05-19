package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class GenericClassUtilsTest {

    @Test
    void generateConcreteClassListString() {
        // Use custom name generator to avoid java.lang package restriction
        Class<?> concreteClass = GenericClassUtils.generateConcreteClass(
            List.class, String.class, () -> "ikaros.test.StringList");
        assertNotNull(concreteClass);
        assertTrue(List.class.isAssignableFrom(concreteClass),
            "Generated class should be assignable from List");
    }

    @Test
    void generateConcreteClassWithCustomNameGenerator() {
        Class<?> concreteClass = GenericClassUtils.generateConcreteClass(
            List.class, String.class, () -> "MyCustomStringList");
        assertNotNull(concreteClass);
        assertEquals("MyCustomStringList", concreteClass.getName());
        assertTrue(List.class.isAssignableFrom(concreteClass),
            "Generated class should be assignable from List");
    }
}
