package run.ikaros.api.infra.utils;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import jakarta.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.File;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import run.ikaros.api.constant.FileConst;
import run.ikaros.api.store.enums.FileType;

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

    private static final String BASE_UPLOAD_DIR_NAME = "upload";

    private static final String BASE_UPLOAD_DIR_PATH
        = SystemVarUtils.getCurrentAppDirPath() + File.separator + BASE_UPLOAD_DIR_NAME;

    /**
     * 构建基础的上传路径.
     *
     * @param uploadedTime 条目数据上传的时间
     * @return 基础的上传目录路径，格式：[upload/yyyy/MM/dd/HH]
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
     * Parse file type by postfix.
     *
     * @param postfix file name postfix
     * @return file name postfix
     */
    public static FileType parseTypeByPostfix(String postfix) {
        Assert.hasText(postfix, "'postfix' must not be blank");
        postfix = postfix.startsWith(".") ? postfix.substring(1) : postfix;
        postfix = postfix.toLowerCase(Locale.ROOT);
        if (IMAGES.contains(postfix)) {
            return FileType.IMAGE;
        }
        if (DOCUMENTS.contains(postfix)) {
            return FileType.DOCUMENT;
        }
        if (VIDEOS.contains(postfix)) {
            return FileType.VIDEO;
        }
        if (VOICES.contains(postfix)) {
            return FileType.VOICE;
        }
        return FileType.UNKNOWN;
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
        int dotIndex = originalFilename.lastIndexOf(".");
        return originalFilename.substring(dotIndex + 1);
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
        try (RandomAccessFile accessFile = new RandomAccessFile(filePath.toFile(), "r")) {
            Long total = 0L;
            byte[] bytes = new byte[size];
            while (accessFile.read(bytes, 0, size) > 0) {
                total += size;
                Path targetFilePath = targetDirPath.resolve(String.valueOf(total));
                Files.write(targetFilePath, bytes);
                paths.add(targetFilePath);
                log.debug("current split file: {}/{}", total, filePath.toFile().length());
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
    public static Flux<DataBuffer> convertToDataBufferFlux(File file) throws IOException {
        DefaultDataBufferFactory bufferFactory = new DefaultDataBufferFactory();
        return DataBufferUtils.readInputStream(() ->
            Files.newInputStream(file.toPath()), bufferFactory, 1024);
    }

    /**
     * Calculate file size.
     */
    public static Long calculateFileSize(Flux<DataBuffer> dataBufferFlux) {
        AtomicLong size = new AtomicLong();
        dataBufferFlux.map(DataBuffer::readableByteCount)
            .reduce(0L, Long::sum)
            .subscribe(size::set);
        return size.get();
    }

    /**
     * Calculate file hash.
     */
    public static String calculateFileHash(Flux<DataBuffer> dataBufferFlux)
        throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");

        dataBufferFlux.subscribe(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            messageDigest.update(bytes);
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
}
