package run.ikaros.server.infra.utils;

import jakarta.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.Assert;
import run.ikaros.server.core.file.FileConst;
import run.ikaros.server.store.enums.FileType;

public class FileUtils {

    static final Set<String> IMAGES =
        Arrays.stream(FileConst.Postfix.IMAGES).collect(Collectors.toSet());
    static final Set<String> DOCUMENTS =
        Arrays.stream(FileConst.Postfix.DOCUMENTS).collect(Collectors.toSet());
    static final Set<String> VIDEOS =
        Arrays.stream(FileConst.Postfix.VIDEOS).collect(Collectors.toSet());
    static final Set<String> VOICES =
        Arrays.stream(FileConst.Postfix.VOICES).collect(Collectors.toSet());

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

}
