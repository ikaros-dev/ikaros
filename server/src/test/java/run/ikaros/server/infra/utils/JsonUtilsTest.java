package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JsonUtilsTest {

    @Test
    void obj2Json_simpleObject() {
        TestObject obj = new TestObject("test", 123);
        String json = JsonUtils.obj2Json(obj);
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"test\""));
        assertTrue(json.contains("\"value\":123"));
    }

    @Test
    void obj2Json_nullField_excluded() {
        TestObject obj = new TestObject(null, 123);
        String json = JsonUtils.obj2Json(obj);
        assertTrue(json.contains("\"value\":123"));
        assertTrue(!json.contains("name"));
    }

    @Test
    void obj2Bytes_simpleObject() {
        TestObject obj = new TestObject("test", 123);
        byte[] bytes = JsonUtils.obj2Bytes(obj);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void json2obj_simpleObject() {
        String json = "{\"name\":\"test\",\"value\":123}";
        TestObject obj = JsonUtils.json2obj(json, TestObject.class);
        assertNotNull(obj);
        assertEquals("test", obj.getName());
        assertEquals(123, obj.getValue());
    }

    @Test
    void json2obj_unknownFields_ignored() {
        String json = "{\"name\":\"test\",\"value\":123,\"unknown\":\"field\"}";
        TestObject obj = JsonUtils.json2obj(json, TestObject.class);
        assertNotNull(obj);
        assertEquals("test", obj.getName());
    }

    @Test
    void json2ObjArr_listOfObjects() {
        String json = "[{\"name\":\"a\",\"value\":1},{\"name\":\"b\",\"value\":2}]";
        TestObject[] arr = JsonUtils.json2ObjArr(json, new TypeReference<TestObject[]>() {});
        assertNotNull(arr);
        assertEquals(2, arr.length);
        assertEquals("a", arr[0].getName());
        assertEquals("b", arr[1].getName());
    }

    @Test
    void obj2Arr_listConversion() {
        List<TestObject> list = List.of(
            new TestObject("a", 1),
            new TestObject("b", 2)
        );
        TestObject[] arr = JsonUtils.obj2Arr(list, new TypeReference<TestObject[]>() {});
        assertNotNull(arr);
        assertEquals(2, arr.length);
    }

    @Test
    void obj2Json_emptyList() {
        String json = JsonUtils.obj2Json(List.of());
        assertEquals("[]", json);
    }

    @Test
    void json2obj_simpleMap() {
        String json = "{\"key\":\"value\",\"number\":42}";
        Map<String, Object> map = JsonUtils.json2obj(json, Map.class);
        assertNotNull(map);
        assertEquals("value", map.get("key"));
    }

    @Test
    void getObjectMapper_notNull() {
        assertNotNull(JsonUtils.getObjectMapper());
    }

    static class TestObject {
        private String name;
        private Integer value;

        public TestObject() {
        }

        public TestObject(String name, Integer value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }
    }
}
