package cn.liguohao.ikaros.common.kit;

import cn.liguohao.ikaros.common.Assert;
import cn.liguohao.ikaros.common.constants.FilePostfixConstants;
import cn.liguohao.ikaros.exceptions.IkarosRuntimeException;
import cn.liguohao.ikaros.model.file.IkarosFile;
import io.jsonwebtoken.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;

/**
 * @author guohao
 * @date 2022/09/07
 */
public class FileKit {

    static final Set<String> IMAGES =
        Arrays.stream(FilePostfixConstants.IMAGES).collect(Collectors.toSet());
    static final Set<String> DOCUMENTS =
        Arrays.stream(FilePostfixConstants.DOCUMENTS).collect(Collectors.toSet());
    static final Set<String> VIDEOS =
        Arrays.stream(FilePostfixConstants.VIDEOS).collect(Collectors.toSet());
    static final Set<String> VOICES =
        Arrays.stream(FilePostfixConstants.VOICES).collect(Collectors.toSet());

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

    public static IkarosFile.Type parseTypeByPostfix(String postfix) {
        Assert.isNotBlank(postfix);
        postfix = postfix.toLowerCase(Locale.ROOT);
        if (IMAGES.contains(postfix)) {
            return IkarosFile.Type.IMAGE;
        }
        if (DOCUMENTS.contains(postfix)) {
            return IkarosFile.Type.DOCUMENT;
        }
        if (VIDEOS.contains(postfix)) {
            return IkarosFile.Type.VIDEO;
        }
        if (VOICES.contains(postfix)) {
            return IkarosFile.Type.VOICE;
        }
        return IkarosFile.Type.UNKNOWN;
    }

    public static byte[] checksum(byte[] bytes, FileKit.Hash hash) throws IkarosRuntimeException {
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
            throw new IkarosRuntimeException(e);
        }
    }

    public static String checksum2Str(byte[] bytes, FileKit.Hash hash)
        throws IkarosRuntimeException {
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

}
