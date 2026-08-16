package run.ikaros.server.config;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.resource.PathResourceResolver;
import org.springframework.web.reactive.resource.ResourceResolver;
import org.springframework.web.reactive.resource.ResourceResolverChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 将驱动虚拟目录安全解析为已注册根目录内的真实文件资源.
 */
@Slf4j
@Component
public class DynamicDirectoryResolver implements ResourceResolver {
    /** 虚拟路径前缀与真实根目录的映射. */
    private final Map<String, Path> directoryMappings = new ConcurrentHashMap<>();

    /** 未匹配动态目录时使用的后备解析器. */
    private final PathResourceResolver fallbackResolver = new PathResourceResolver();

    /**
     * 添加目录映射.
     * 例如：addDirectoryMapping("/uploads/", "/data/uploads")
     * 意味着 /dynamic/uploads/** 映射到 /data/uploads/**
     */
    public void addDirectoryMapping(String virtualPath, String realDirectory) {
        Path realPath;
        try {
            realPath = Path.of(realDirectory).toAbsolutePath().normalize().toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException("挂载目录不存在或不可访问: " + realDirectory,
                exception);
        }
        if (!Files.isDirectory(realPath) || !Files.isReadable(realPath)) {
            throw new IllegalArgumentException("挂载路径不是可读目录: " + realDirectory);
        }
        directoryMappings.put(virtualPath, realPath);
        log.debug("添加目录映射: {} -> {}", virtualPath, realPath);
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
        @Nullable ServerWebExchange exchange,
        String requestPath,
        List<? extends Resource> locations,
        ResourceResolverChain chain) {


        requestPath = requestPath.replace("%20", " ")
            .replace("%2F", "/")
            .replace("%3A", ":")
            .replace("%3F", "?")
            .replace("%26", "&")
            .replace("%23", "#");
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

            String normalizedPrefix = virtualPrefix.endsWith("/")
                ? virtualPrefix.substring(0, virtualPrefix.length() - 1)
                : virtualPrefix;
            if (!requestPath.startsWith(normalizedPrefix + "/")) {
                continue;
            }
            String relativePath = requestPath.substring(normalizedPrefix.length() + 1);
            Path candidatePath = realBaseDir.resolve(relativePath).normalize();
            if (!candidatePath.startsWith(realBaseDir)) {
                log.warn("安全警告: 路径遍历尝试 {}", candidatePath);
                continue;
            }
            try {
                Path realFilePath = candidatePath.toRealPath();
                if (!realFilePath.startsWith(realBaseDir)) {
                    log.warn("安全警告: 文件真实路径超出挂载目录 {}", realFilePath);
                    continue;
                }
                if (Files.isReadable(realFilePath)) {
                    Resource resource = new FileSystemResource(realFilePath);
                    log.debug("找到资源: {}", realFilePath);
                    return Mono.just(resource);
                }
            } catch (IOException exception) {
                log.debug("资源不存在或不可访问: {}", candidatePath);
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
