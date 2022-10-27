package run.ikaros.server.utils;

import io.jsonwebtoken.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import run.ikaros.server.constants.FileConst;
import run.ikaros.server.enums.FileType;
import run.ikaros.server.exceptions.RuntimeIkarosException;

/**
 * @author guohao
 * @date 2022/09/07
 */
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

    public enum Hash {
        MD5("MD5"),
        SHA1("SHA1"),
        SHA256("SHA-256"),
        SHA512("SHA-512");
        private String name;

        Hash(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
    }

    public static FileType parseTypeByPostfix(String postfix) {
        AssertUtils.notBlank(postfix, "'postfix' must not be blank");
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




    public static byte[] checksum(byte[] bytes, FileUtils.Hash hash) throws RuntimeIkarosException {
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
            throw new RuntimeIkarosException(e);
        }
    }

    public static String checksum2Str(byte[] bytes, FileUtils.Hash hash)
        throws RuntimeIkarosException {
        return DatatypeConverter.printHexBinary(checksum(bytes, hash));
    }

    public static void deleteDirByRecursion(String dirPath) {
        File parentFile = new File(dirPath);
        boolean deleteSuccess = true;
        File[] files = parentFile.listFiles();
        if (files == null || files.length == 0) {
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
     * @return 条目数据的文件路径，格式：[upload/yyyy/MM/dd/HH/随机生成的UUID.postfix]
     */
    public static String buildAppUploadFilePath(String postfix) {
        AssertUtils.notBlank(postfix, "'postfix' must not be blank");
        return buildAppUploadFileBasePath(LocalDateTime.now())
            + File.separator + UUID.randomUUID().toString().replace("-", "")
            + (('.' == postfix.charAt(0))
            ? postfix : "." + postfix);
    }

    /**
     * @param uploadedTime 条目数据上传的时间
     * @return 基础的上传目录路径，格式：[upload/yyyy/MM/dd/HH]
     */
    public static String buildAppUploadFileBasePath(LocalDateTime uploadedTime) {
        AssertUtils.notNull(uploadedTime, "'uploadedTime' must not be null");
        String locationDirPath = BASE_UPLOAD_DIR_PATH
            + File.separator + uploadedTime.getYear()
            + File.separator + uploadedTime.getMonthValue()
            + File.separator + uploadedTime.getDayOfMonth()
            + File.separator + uploadedTime.getHour();

        File locationDir = new File(locationDirPath);
        if (!locationDir.exists()) {
            locationDir.mkdirs();
        }
        return locationDirPath;
    }

    public static String parseFilePostfix(String originalFilename) {
        AssertUtils.notBlank(originalFilename, "originalFilename");
        int dotIndex = originalFilename.lastIndexOf(".");
        return originalFilename.substring(dotIndex + 1);
    }

}
