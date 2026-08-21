package run.ikaros.api.infra.utils;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.xml.bind.DatatypeConverter;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.FileConst;

@Slf4j
public class FileUtils {

    static final Set<String> IMAGES =
        Arrays.stream(FileConst.Postfix.IMAGES).collect(Collectors.toSet());
    static final Set<String> DOCUMENTS =
        Arrays.stream(FileConst.Postfix.DOCUMENTS).collect(Collectors.toSet());
    static final Set<String> VIDEOS =
        Arrays.stream(FileConst.Postfix.VIDEOS).collect(Collectors.toSet());
    static final Set<String> VOICES =
        Arrays.stream(FileConst.Postfix.VOICES).collect(Collectors.toSet());

    private static final String BASE_UPLOAD_DIR_NAME = FileConst.DEFAULT_DIR_NAME;

    private static final String BASE_UPLOAD_DIR_PATH
        = SystemVarUtils.getCurrentAppDirPath() + File.separator + BASE_UPLOAD_DIR_NAME;

    /**
     * 构建基础的上传路径.
     *
     * @param uploadedTime 条目数据上传的时间
     * @return 基础的上传目录路径，格式：[files/yyyy/MM/dd/HH]
     */
    public static String buildAppUploadFileBasePath(String basePath, LocalDateTime uploadedTime) {
        Assert.notNull(uploadedTime, "'uploadedTime' must not be null");
        String locationDirPath =
            (StringUtils.hasText(basePath) ? basePath + File.separator + BASE_UPLOAD_DIR_NAME
                : BASE_UPLOAD_DIR_PATH)
                + File.separator + uploadedTime.getYear()
                + File.separator + uploadedTime.getMonthValue()
                + File.separator + uploadedTime.getDayOfMonth();

        File locationDir = new File(locationDirPath);
        if (!locationDir.exists()) {
            locationDir.mkdirs();
        }
        return locationDirPath;
    }


    /**
     * 条目数据的文件路径，格式：[upload/yyyy/MM/dd/HH/随机生成的UUID.postfix].
     */
    public static String buildAppUploadFilePath(@Nullable String basePath,
                                                @NotBlank String postfix) {
        Assert.hasText(postfix, "'postfix' must not be blank");
        return buildAppUploadFileBasePath(basePath, LocalDateTime.now())
            + File.separator + UUID.randomUUID().toString().replace("-", "")
            + (('.' == postfix.charAt(0))
            ? postfix : "." + postfix);
    }

    /**
     * Create dir if not exists.
     */
    public static boolean mkdirsIfNotExists(Path dirPath) {
        Assert.notNull(dirPath, "'dirPath' must not null.");
        File file = dirPath.toFile();
        if (!file.exists()) {
            return file.mkdirs();
        }
        return false;
    }

    public static boolean isVideo(String url) {
        return VIDEOS.contains(parseFilePostfix(url));
    }

    public static boolean isDocument(String url) {
        return DOCUMENTS.contains(parseFilePostfix(url));
    }

    public static boolean isVoice(String url) {
        return VOICES.contains(parseFilePostfix(url));
    }

    public static boolean isImage(String url) {
        return IMAGES.contains(parseFilePostfix(url));
    }

    public enum Hash {
        MD5("MD5"),
        SHA1("SHA1"),
        SHA256("SHA-256"),
        SHA512("SHA-512");
        private final String name;

        Hash(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }


    /**
     * Check bytes sum by appoint {@link Hash}.
     *
     * @param bytes byte arr
     * @param hash  hash
     * @return byte arr sum result
     */
    public static byte[] checksum(byte[] bytes, Hash hash) {
        try (InputStream in = new ByteArrayInputStream(bytes)) {
            MessageDigest digest = MessageDigest.getInstance(hash.getName());
            byte[] block = new byte[4096];
            int length;
            while ((length = in.read(block)) > 0) {
                digest.update(block, 0, length);
            }
            return digest.digest();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public static String checksum2Str(byte[] bytes, Hash hash) {
        return DatatypeConverter.printHexBinary(checksum(bytes, hash));
    }

    /**
     * Delete dir by recursion.
     */
    public static void deleteDirByRecursion(String dirPath) throws IOException {
        File parentFile = new File(dirPath);
        boolean deleteSuccess = true;
        File[] files = parentFile.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            deleteDirByRecursion(file.getPath());
            if (file.exists()) {
                deleteSuccess = file.delete();
            }
            if (!deleteSuccess) {
                throw new IOException("delete file fail, current path: " + file.getAbsolutePath());
            }
        }
    }

    /**
     * Parse file name postfix.
     *
     * @param originalFilename original file name
     * @return postfix
     */
    public static String parseFilePostfix(String originalFilename) {
        Assert.hasText(originalFilename, "originalFilename");
        if (originalFilename.indexOf("?") > 0) {
            originalFilename = originalFilename.substring(0, originalFilename.indexOf("?"));
        }
        int dotIndex = originalFilename.lastIndexOf(".");
        return originalFilename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Parse file name without postfix.
     */
    public static String parseFileNameWithoutPostfix(String originalFilename) {
        Assert.hasText(originalFilename, "originalFilename");
        int dotIndex = originalFilename.lastIndexOf(".");
        return originalFilename.substring(0, dotIndex);
    }

    public static String parseFileName(String filePath) {
        Assert.hasText(filePath, "filePath");
        return filePath.substring(filePath.lastIndexOf("/") + 1);
    }

    /**
     * remove char \ / : * ? " < > | .
     */
    public static String formatDirName(String dirName) {
        Assert.hasText(dirName, "dir name");
        return dirName
            .replace("\\", "")
            .replace("/", "")
            .replace(":", "")
            .replace("*", "")
            .replace("?", "")
            .replace("\"", "")
            .replace("<", "")
            .replace(">", "")
            .replace("|", "");
    }


    public static void deletePathAndContentIfExists(Path path) throws IOException {
        deleteFileAndChild(path.toFile());
    }

    private static void deleteFileAndChild(File parentFile) throws IOException {
        if (parentFile == null) {
            return;
        }

        if (parentFile.isFile()) {
            Files.deleteIfExists(parentFile.toPath());
            log.debug("Delete file in path: {}", parentFile.toPath());
        }

        if (parentFile.listFiles() == null) {
            return;
        }
        for (File file : Objects.requireNonNull(parentFile.listFiles())) {
            deleteFileAndChild(file);
            Files.deleteIfExists(file.toPath());
            log.debug("Delete file in path: {}", file.toPath());
        }
    }


    /**
     * Split file to target dir multi chunk.
     *
     * @param filePath      original file path
     * @param targetDirPath target dir path
     * @param size          unit is KB
     * @return multi chunk paths
     */
    public static List<Path> split(Path filePath, Path targetDirPath, Integer size) {
        List<Path> paths = new ArrayList<>();
        size = size * 1024;
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             BufferedInputStream bis = new BufferedInputStream(fis)) {
            byte[] buffer = new byte[size];
            int bytesRead;
            int chunkIndex = 0;
            while ((bytesRead = bis.read(buffer)) != -1) {
                Path targetPath = targetDirPath.resolve(String.valueOf(chunkIndex));
                FileOutputStream fos = new FileOutputStream(targetPath.toFile());
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                bos.write(buffer, 0, bytesRead);
                bos.close();
                chunkIndex++;
                paths.add(targetPath);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return paths;
    }

    /**
     * Synthesize all chunks files.
     *
     * @param chunkFilePaths chunk file paths
     */
    public static void synthesize(List<Path> chunkFilePaths, Path targetFilePath) {
        File targetFile = targetFilePath.toFile();
        if (targetFile.exists()) {
            throw new RuntimeException(
                "target file has exists: " + targetFile.getAbsolutePath());
        }

        if (!targetFile.getParentFile().exists()) {
            targetFile.getParentFile().mkdirs();
        }

        try {
            if (!targetFile.createNewFile()) {
                throw new RuntimeException(
                    "create target file fail, path: " + targetFile.getAbsolutePath());
            }
            try (RandomAccessFile accessFile = new RandomAccessFile(targetFile,
                "rw")) {
                long total = 0L;
                List<Path> pathSortedList = chunkFilePaths.stream()
                    .sorted((o1, o2) -> (int) (Long.parseLong(o1.toFile().getName())
                        - Long.parseLong(o2.toFile().getName()))).toList();
                for (Path path : pathSortedList) {
                    File file = path.toFile();
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    accessFile.write(bytes);
                    total += bytes.length;
                    log.debug("current write total: {} for target file: {}",
                        total, targetFile.getAbsolutePath());
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convert file to data buffer flux.
     */
    public static Flux<DataBuffer> convertToDataBufferFlux(File file) {
        DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        return DataBufferUtils.readInputStream(() ->
            Files.newInputStream(file.toPath()), bufferFactory, 1024);
    }

    /**
     * Calculate file size.
     */
    public static Mono<Long> calculateFileSize(Flux<DataBuffer> dataBufferFlux) {
        return dataBufferFlux.map(DataBuffer::readableByteCount)
            .reduce(0L, Long::sum);
    }

    /**
     * Calculate file hash.
     */
    public static String calculateFileHash(Flux<DataBuffer> dataBufferFlux) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        MessageDigest finalMessageDigest = messageDigest;
        dataBufferFlux.subscribe(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            finalMessageDigest.update(bytes);
        });

        byte[] hashBytes = messageDigest.digest();
        return bytesToHex(hashBytes);
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Convert path to url.
     */
    public static String path2url(@NotBlank String path, @Nullable String workDir) {
        Assert.hasText(path, "'path' must has text.");
        String url = "";
        String currentAppDirPath =
            StringUtils.hasText(workDir) ? workDir : SystemVarUtils.getCurrentAppDirPath();
        url = path.replace(currentAppDirPath, "");
        // 如果是ntfs目录，则需要替换下 \ 为 /
        if (SystemVarUtils.platformIsWindows()) {
            url = url.replace("\\", "/");
        }
        log.debug("current url={}", url);
        return url;
    }

    /**
     * 计算SHA1的值
     * .
     */
    public static String calculateSha1(String filePath)
        throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");

        try (FileInputStream fis = new FileInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] sha1Bytes = digest.digest();
        return bytesToHex(sha1Bytes);
    }

}
