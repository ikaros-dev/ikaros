package cn.liguohao.ikaros.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * json转换工具类
 *
 * @author li-guohao
 */
public class JacksonConverter {
    private static final Logger logger = LoggerFactory.getLogger(JacksonConverter.class);

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    /**
     * 转换对象为JSON
     *
     * @param obj 待转换的对象
     * @return JSON
     */
    public static String obj2Json(Object obj) {
        Assert.isNotNull(obj);
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("convert obj to json fail. ", e);
        }
        return null;
    }

    /**
     * 转换JSON为对象
     *
     * @param json  json字符串
     * @param clazz 对象字节码
     * @param <T>   对象类型
     * @return 对象实例
     */
    public static <T> T json2obj(String json, Class<T> clazz) {
        Assert.isNotNull(json, clazz);
        Assert.isNotBlank(json);
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("convert json to obj fail. ", e);
        }
        return null;
    }

    /**
     * 转换JSON为对象数组
     *
     * @param json          json字符串
     * @param typeReference 对象类型引用
     * @param <T>           对象类型
     * @return 对象实例
     */
    public static <T> T[] json2ObjArr(String json, TypeReference<T[]> typeReference) {
        Assert.isNotNull(json, typeReference);
        Assert.isNotBlank(json);
        try {
            T[] ts = objectMapper.readValue(json.getBytes(StandardCharsets.UTF_8), typeReference);
            return ts;
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("convert json to obj arr fail. ", e);
        } catch (IOException e) {
            e.printStackTrace();
            logger.error("convert json to obj arr fail. ", e);
        }

        return null;
    }

    /**
     * 对象转指定的类型数组
     *
     * @param obj           待转换的对象
     * @param typeReference 对象类型引用
     * @param <T>           对象类型
     * @return 对象实例
     */
    public static <T> T[] obj2Arr(Object obj, TypeReference<T[]> typeReference) {
        Assert.isNotNull(obj, typeReference);
        return json2ObjArr(obj2Json(obj), typeReference);
    }

}