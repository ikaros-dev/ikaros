package run.ikaros.server.infra.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    void obj2Json_nullObject_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> JsonUtils.obj2Json(null));
    }

    @Test
    void obj2Json_emptyObject() {
        TestObject obj = new TestObject(null, null);
        String json = JsonUtils.obj2Json(obj);
        assertNotNull(json);
        assertEquals("{}", json);
    }

    @Test
    void obj2Json_listOfObjects() {
        List<TestObject> list = List.of(
            new TestObject("a", 1),
            new TestObject("b", 2)
        );
        String json = JsonUtils.obj2Json(list);
        assertNotNull(json);
        assertTrue(json.contains("\"name\":\"a\""));
        assertTrue(json.contains("\"name\":\"b\""));
    }

    @Test
    void obj2Bytes_simpleObject() {
        TestObject obj = new TestObject("test", 123);
        byte[] bytes = JsonUtils.obj2Bytes(obj);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);
    }

    @Test
    void obj2Bytes_nullObject_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> JsonUtils.obj2Bytes(null));
    }

    @Test
    void obj2Bytes_containsCorrectData() {
        TestObject obj = new TestObject("hello", 456);
        byte[] bytes = JsonUtils.obj2Bytes(obj);
        String json = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(json.contains("\"name\":\"hello\""));
        assertTrue(json.contains("\"value\":456"));
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
    void json2obj_nullJson_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> JsonUtils.json2obj(null, TestObject.class));
    }

    @Test
    void json2obj_nullClass_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> JsonUtils.json2obj("{}", null));
    }

    @Test
    void json2obj_invalidJson_returnsNull() {
        String json = "{invalid json}";
        TestObject obj = JsonUtils.json2obj(json, TestObject.class);
        assertNull(obj);
    }

    @Test
    void json2obj_emptyJson() {
        String json = "{}";
        TestObject obj = JsonUtils.json2obj(json, TestObject.class);
        assertNotNull(obj);
        assertNull(obj.getName());
        assertNull(obj.getValue());
    }

    @Test
    void json2obj_nestedObject() {
        String json = "{\"name\":\"test\",\"nested\":{\"value\":123}}";
        Map<String, Object> map = JsonUtils.json2obj(json, Map.class);
        assertNotNull(map);
        assertEquals("test", map.get("name"));
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
    void json2ObjArr_nullJson_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> JsonUtils.json2ObjArr(null, new TypeReference<TestObject[]>() {}));
    }

    @Test
    void json2ObjArr_nullTypeReference_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> JsonUtils.json2ObjArr("[]", null));
    }

    @Test
    void json2ObjArr_invalidJson_returnsNull() {
        String json = "{invalid}";
        TestObject[] arr = JsonUtils.json2ObjArr(json, new TypeReference<TestObject[]>() {});
        assertNull(arr);
    }

    @Test
    void json2ObjArr_emptyArray() {
        String json = "[]";
        TestObject[] arr = JsonUtils.json2ObjArr(json, new TypeReference<TestObject[]>() {});
        assertNotNull(arr);
        assertEquals(0, arr.length);
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
    void obj2Arr_nullObject_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> JsonUtils.obj2Arr(null, new TypeReference<TestObject[]>() {}));
    }

    @Test
    void obj2Arr_nullTypeReference_throwsException() {
        assertThrows(IllegalArgumentException.class,
            () -> JsonUtils.obj2Arr(new Object(), null));
    }

    @Test
    void obj2Arr_emptyList() {
        TestObject[] arr = JsonUtils.obj2Arr(List.of(), new TypeReference<TestObject[]>() {});
        assertNotNull(arr);
        assertEquals(0, arr.length);
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
    void json2obj_nullMap() {
        String json = "null";
        Map<String, Object> map = JsonUtils.json2obj(json, Map.class);
        assertNull(map);
    }

    @Test
    void getObjectMapper_notNull() {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        assertNotNull(mapper);
    }

    @Test
    void objectMapper_hasJavaTimeModule() throws com.fasterxml.jackson.core.JsonProcessingException {
        ObjectMapper mapper = JsonUtils.getObjectMapper();
        String json = mapper.writeValueAsString(java.time.LocalDateTime.of(2024, 1, 15, 10, 30));
        assertNotNull(json);
        assertTrue(json.contains("2024"));
    }

    @Test
    void json2obj_withJavaTime() {
        String json = "{\"name\":\"test\",\"createTime\":\"2024-01-15T10:30:00\"}";
        TestObjectWithTime obj = JsonUtils.json2obj(json, TestObjectWithTime.class);
        assertNotNull(obj);
        assertEquals("test", obj.getName());
        assertNotNull(obj.getCreateTime());
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

    static class TestObjectWithTime {
        private String name;
        private java.time.LocalDateTime createTime;

        public TestObjectWithTime() {
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public java.time.LocalDateTime getCreateTime() {
            return createTime;
        }

        public void setCreateTime(java.time.LocalDateTime createTime) {
            this.createTime = createTime;
        }
    }
}
