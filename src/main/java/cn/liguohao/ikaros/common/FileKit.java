package cn.liguohao.ikaros.common;

import cn.liguohao.ikaros.exceptions.IkarosRuntimeException;
import cn.liguohao.ikaros.file.IkarosFile;
import jakarta.xml.bind.DatatypeConverter;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author guohao
 * @date 2022/09/07
 */
public class FileKit {

    static final Set<String> IMAGES =
        Arrays.stream(FilePostfixConstant.IMAGES).collect(Collectors.toSet());
    static final Set<String> DOCUMENTS =
        Arrays.stream(FilePostfixConstant.DOCUMENTS).collect(Collectors.toSet());
    static final Set<String> VIDEOS =
        Arrays.stream(FilePostfixConstant.VIDEOS).collect(Collectors.toSet());
    static final Set<String> VOICES =
        Arrays.stream(FilePostfixConstant.VOICES).collect(Collectors.toSet());

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

}
