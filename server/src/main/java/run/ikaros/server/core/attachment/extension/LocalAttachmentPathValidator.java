package run.ikaros.server.core.attachment.extension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 管理本地附件驱动的可信根目录，并阻止文件访问逃逸出对应根目录.
 */
@Component
public class LocalAttachmentPathValidator {
    /** 已启用本地驱动与其真实根目录的映射. */
    private final Map<UUID, Path> driverRootPaths = new ConcurrentHashMap<>();

    /**
     * 注册已启用本地驱动的可信根目录.
     *
     * @param driverId 驱动 ID
     * @param rootPath 驱动根目录
     */
    public void register(UUID driverId, String rootPath) {
        Assert.notNull(driverId, "driverId must not be null.");
        Assert.hasText(rootPath, "rootPath must not be empty.");
        Path realRootPath = toRealPath(toPath(rootPath), "驱动根目录不存在或不可访问: ");
        if (!Files.isDirectory(realRootPath) || !Files.isReadable(realRootPath)) {
            throw new IllegalArgumentException("驱动根目录不是可读目录: " + rootPath);
        }
        driverRootPaths.put(driverId, realRootPath);
    }

    /**
     * 移除本地驱动的可信根目录.
     *
     * @param driverId 驱动 ID
     */
    public void unregister(UUID driverId) {
        if (driverId != null) {
            driverRootPaths.remove(driverId);
        }
    }

    /**
     * 校验目标路径属于指定驱动的可信根目录.
     *
     * @param driverId 驱动 ID
     * @param targetPath 目标路径
     * @return 校验后的真实路径
     */
    public Mono<Path> validate(UUID driverId, String targetPath) {
        return Mono.fromCallable(() -> validateNow(driverId, targetPath))
            .subscribeOn(Schedulers.boundedElastic());
    }

    Path validateNow(UUID driverId, String targetPath) {
        Assert.notNull(driverId, "driverId must not be null.");
        Assert.hasText(targetPath, "targetPath must not be empty.");
        Path realRootPath = driverRootPaths.get(driverId);
        if (realRootPath == null) {
            throw new IllegalStateException("本地附件驱动未启用或未注册: " + driverId);
        }
        Path realTargetPath = toRealPath(toPath(targetPath), "目标路径不存在或不可访问: ");
        if (!realTargetPath.startsWith(realRootPath)) {
            throw new IllegalArgumentException("目标路径超出驱动根目录: " + targetPath);
        }
        return realTargetPath;
    }

    /**
     * 将本地路径字符串转换为路径，并拒绝路径段之间混用的连续分隔符.
     *
     * @param path 本地路径字符串
     * @return 转换后的路径
     */
    Path toPath(String path) {
        if (path.length() >= 3 && Character.isLetter(path.charAt(0))
            && path.charAt(1) == ':' && isSeparator(path.charAt(2))) {
            int separatorStart = 2;
            boolean rootSeparators = true;
            while (separatorStart < path.length()) {
                if (!isSeparator(path.charAt(separatorStart))) {
                    separatorStart++;
                    continue;
                }
                int separatorEnd = separatorStart;
                boolean containsSlash = false;
                boolean containsBackslash = false;
                while (separatorEnd < path.length() && isSeparator(path.charAt(separatorEnd))) {
                    containsSlash |= path.charAt(separatorEnd) == '/';
                    containsBackslash |= path.charAt(separatorEnd) == '\\';
                    separatorEnd++;
                }
                if (!rootSeparators && containsSlash && containsBackslash) {
                    throw new IllegalArgumentException("路径段之间不能混用连续分隔符: " + path);
                }
                rootSeparators = false;
                separatorStart = separatorEnd;
            }
        }
        return Path.of(path);
    }

    private boolean isSeparator(char character) {
        return character == '/' || character == '\\';
    }

    private Path toRealPath(Path path, String messagePrefix) {
        try {
            return path.toAbsolutePath().normalize().toRealPath();
        } catch (IOException exception) {
            throw new IllegalArgumentException(messagePrefix + path, exception);
        }
    }
}
