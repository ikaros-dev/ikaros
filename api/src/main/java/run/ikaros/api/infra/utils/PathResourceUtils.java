package run.ikaros.api.infra.utils;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

public class PathResourceUtils {
    /**
     * 根据包名获取下面的所有类.
     *
     * @param basePackage 基础包名
     * @return 包下的所有类
     */
    public static List<Class<?>> getClasses(String basePackage) throws Exception {
        List<Class<?>> classes = new ArrayList<>();
        // 1. 创建资源解析器
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 2. 将包名转换为资源路径格式 (例如 com.example -> classpath*:com/example/**/*.class)
        String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX
            + ClassUtils.convertClassNameToResourcePath(basePackage) + "/**/*.class";

        // 3. 获取所有资源
        Resource[] resources = resolver.getResources(packageSearchPath);
        MetadataReaderFactory readerFactory = new CachingMetadataReaderFactory(resolver);

        for (Resource resource : resources) {
            if (resource.isReadable()) {
                // 4. 使用元数据读取器获取类名，避免直接加载 Class 导致的潜在初始化问题
                MetadataReader reader = readerFactory.getMetadataReader(resource);
                String className = reader.getClassMetadata().getClassName();
                classes.add(Class.forName(className));
            }
        }
        return classes;
    }
}
