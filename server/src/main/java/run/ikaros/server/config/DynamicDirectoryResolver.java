package run.ikaros.server.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.resource.PathResourceResolver;
import org.springframework.web.reactive.resource.ResourceResolver;
import org.springframework.web.reactive.resource.ResourceResolverChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class DynamicDirectoryResolver implements ResourceResolver {
    // 存储虚拟路径到真实目录的映射
    // key: 虚拟路径前缀，必须以 / 开头和结尾，如 "/uploads/"
    // value: 对应的真实文件系统目录
    private final Map<String, Path> directoryMappings = new ConcurrentHashMap<>();

    // 用于未匹配时的后备解析器
    private final PathResourceResolver fallbackResolver = new PathResourceResolver();

    /**
     * 添加目录映射.
     * 例如：addDirectoryMapping("/uploads/", "/data/uploads")
     * 意味着 /dynamic/uploads/** 映射到 /data/uploads/**
     */
    public void addDirectoryMapping(String virtualPath, String realDirectory) {
        // 规范化虚拟路径
        // String normalizedVirtualPath = normalizeVirtualPath(virtualPath);
        String normalizedVirtualPath = virtualPath;

        // 创建真实目录路径
        Path realPath = Paths.get(realDirectory).toAbsolutePath().normalize();

        // 确保目录存在
        if (!Files.exists(realPath)) {
            try {
                Files.createDirectories(realPath);
            } catch (Exception e) {
                throw new RuntimeException("无法创建目录: " + realDirectory, e);
            }
        }

        // 存储映射
        directoryMappings.put(normalizedVirtualPath, realPath);
        log.debug("添加目录映射: {} -> {}", normalizedVirtualPath, realDirectory);
    }

    /**
     * 移除目录映射.
     */
    public void removeDirectoryMapping(String virtualPath) {
        directoryMappings.remove(virtualPath);
    }

    /**
     * 获取所有映射.
     */
    public Map<String, Path> getAllMappings() {
        return new ConcurrentHashMap<>(directoryMappings);
    }

    /**
     * 解析资源请求.
     */
    @Override
    public Mono<Resource> resolveResource(
        ServerWebExchange exchange,
        String requestPath,
        List<? extends Resource> locations,
        ResourceResolverChain chain) {


        requestPath = URLDecoder.decode(requestPath, StandardCharsets.UTF_8);
        requestPath = requestPath.replace("%20", " ")
                .replace("%2F", "/")
            .replace("%3A", ":")
            .replace("%3F", "?")
            .replace("%26", "&")
            .replace("%23", "#");
        log.debug("请求路径: {}", requestPath);

        // 遍历所有目录映射
        for (Map.Entry<String, Path> entry : directoryMappings.entrySet()) {
            String virtualPrefix = entry.getKey();
            Path realBaseDir = entry.getValue();

            log.debug("检查映射: {} -> {}", virtualPrefix, realBaseDir);

            // 检查请求路径是否以虚拟前缀开头
            if (requestPath.startsWith(virtualPrefix)) {
                // 提取相对路径部分
                String relativePath = requestPath.substring(virtualPrefix.length() + 1);

                // 构建真实文件路径
                Path realFilePath = realBaseDir.resolve(relativePath).normalize();

                // 安全检查：确保请求的文件在允许的目录内
                if (!realFilePath.startsWith(realBaseDir)) {

                    log.warn("安全警告: 路径遍历尝试 {}", realFilePath);
                    continue;
                }

                // 检查文件是否存在且可读
                if (Files.exists(realFilePath) && Files.isReadable(realFilePath)) {
                    Resource resource = new PathResource(realFilePath);
                    log.debug("找到资源: {}", realFilePath);
                    return Mono.just(resource);
                } else {
                    log.debug("资源不存在: {}", realFilePath);
                }
            }
        }

        // 如果没有找到匹配的资源，尝试后备解析器
        log.debug("未找到匹配的映射，使用后备解析器");
        return fallbackResolver.resolveResource(exchange, requestPath, locations, chain);
    }

    @Override
    public Mono<String> resolveUrlPath(
        String resourcePath,
        List<? extends Resource> locations,
        ResourceResolverChain chain) {
        return fallbackResolver.resolveUrlPath(resourcePath, locations, chain);
    }

}
