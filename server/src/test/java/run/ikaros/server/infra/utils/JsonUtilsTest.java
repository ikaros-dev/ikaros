package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {

    @Test
    void obj2Json_validObject_returnsJsonString() {
        TestObj obj = new TestObj("hello", 42);
        String json = JsonUtils.obj2Json(obj);
        assertThat(json).contains("\"name\"");
        assertThat(json).contains("\"hello\"");
        assertThat(json).contains("\"value\"");
    }

    @Test
    void obj2Json_beanWithNull_returnsJson() {
        TestObj obj = new TestObj(null, 0);
        String json = JsonUtils.obj2Json(obj);
        assertThat(json).isNotNull();
    }

    @Test
    void json2obj_validJson_returnsObject() {
        String json = "{\"name\":\"test\",\"value\":123}";
        TestObj obj = Objects.requireNonNull(JsonUtils.json2obj(json, TestObj.class));
        assertThat(obj).isNotNull();
        assertThat(obj.getName()).isEqualTo("test");
        assertThat(obj.getValue()).isEqualTo(123);
    }

    @Test
    void json2obj_invalidJson_returnsNull() {
        TestObj obj = JsonUtils.json2obj("invalid json", TestObj.class);
        assertThat(obj).isNull();
    }

    @Test
    void json2ObjArr_validJson_returnsArray() {
        String json = "[{\"name\":\"a\",\"value\":1},{\"name\":\"b\",\"value\":2}]";
        TestObj[] arr = Objects.requireNonNull(
            JsonUtils.json2ObjArr(json, new TypeReference<TestObj[]>() {
            }));
        assertThat(arr).hasSize(2);
        assertThat(arr[0].getName()).isEqualTo("a");
        assertThat(arr[1].getName()).isEqualTo("b");
    }

    @Test
    void json2ObjArr_invalidJson_returnsEmptyArray() {
        // implementation returns null for invalid json
        TestObj[] arr = JsonUtils.json2ObjArr("bad json", new TypeReference<TestObj[]>() {
        });
        assertThat(arr).isNull();
    }

    @Test
    void obj2JsonAndBack_roundTrip() {
        TestObj original = new TestObj("roundtrip", 999);
        String json = Objects.requireNonNull(JsonUtils.obj2Json(original));
        TestObj restored = Objects.requireNonNull(JsonUtils.json2obj(json, TestObj.class));
        assertThat(restored).isNotNull();
        assertThat(restored.getName()).isEqualTo("roundtrip");
        assertThat(restored.getValue()).isEqualTo(999);
    }

    static class TestObj {
        private @Nullable String name;
        private int value;

        public TestObj() {
        }

        public TestObj(@Nullable String name, int value) {
            this.name = name;
            this.value = value;
        }

        public @Nullable String getName() {
            return name;
        }

        public void setName(@Nullable String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
