package run.ikaros.server.infra.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.util.Assert;

public class AesEncryptUtils {

    /**
     * 密钥算法.
     */
    private static final String KEY_ALGORITHM = "AES";

    /**
     * 加解密算法/工作模式/填充方式 Java6.0支持PKCS5Padding填充方式,
     * BouncyCastle支持PKCS7Padding填充方式.
     */
    private static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

    /**
     * AES密钥长度默认值 192.
     */
    private static final int DEFAULT_AES_KEY_LENGTH = 192;


    /**
     * 生成密钥字符数组.
     *
     * @param aesKeyLength 密钥长度 AES算法要求密钥长度位 128 192 256
     * @return 已经用Base64编码的密钥字节数组
     */
    public static byte[] generateKeyByteArray(int aesKeyLength) {
        // 获取密钥生成器
        KeyGenerator keyGenerator = null;
        try {
            keyGenerator = KeyGenerator.getInstance(KEY_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        // 初始化生成器 AES算法要求密钥长度位 128 192 256 默认192
        keyGenerator.init(aesKeyLength);

        // 生成密钥
        SecretKey secretKey = keyGenerator.generateKey();

        // 返回密钥字节数组
        return Base64.getEncoder().encode(secretKey.getEncoded());
    }

    /**
     * 生成密钥字符数组.
     *
     * @return 已经用Base64编码的密钥字节数组
     * @see #generateKeyByteArray(int)
     */
    public static byte[] generateKeyByteArray() {
        return generateKeyByteArray(DEFAULT_AES_KEY_LENGTH);
    }

    /**
     * 生成密钥文件.
     *
     * @param aesKeyLength 密钥长度 AES算法要求密钥长度位 128 192 256
     * @param keyFile      密钥文件
     * @throws IOException 文件IO操作异常
     * @see #generateKeyByteArray(int)
     */
    public static void generateKeyFile(int aesKeyLength, File keyFile) throws IOException {
        // 父目录判断
        if (!keyFile.getParentFile().exists()) {
            keyFile.getParentFile().mkdirs();
        }

        // 生成密钥
        byte[] bytes = generateKeyByteArray(aesKeyLength);

        // 输出到文件
        FileOutputStream fileOutputStream = new FileOutputStream(keyFile);
        fileOutputStream.write(bytes);

        // 释放流
        fileOutputStream.close();
    }

    /**
     * 生成密钥文件.
     *
     * @param keyFile 密钥文件
     * @throws IOException 文件IO异常
     * @see #generateKeyFile(int, File)
     */
    public static void generateKeyFile(File keyFile) throws IOException {
        generateKeyFile(DEFAULT_AES_KEY_LENGTH, keyFile);
    }

    /**
     * Encrypt by stream.
     */
    public static void encryptInputStream(InputStream data, boolean isClose,
                                          OutputStream out, byte[] keyBytes)
        throws IOException {
        Assert.notNull(data, "'data' must not null.");
        Assert.notNull(out, "'out' must not null.");
        Assert.notNull(keyBytes, "'keyBytes' must not null.");
        Key key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        try (out) {
            // 获取Cipher对象
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            // 设置加密模式
            cipher.init(Cipher.ENCRYPT_MODE, key);
            // 创建加密输出流
            CipherOutputStream cipherOutputStream = new CipherOutputStream(out, cipher);
            // 加密数据流
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = data.read(buffer)) != -1) {
                cipherOutputStream.write(buffer, 0, bytesRead);
            }
            cipherOutputStream.close();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        } finally {
            if (isClose) {
                data.close();
            }
        }

    }

    /**
     * Decrypt by stream.
     */
    public static void decryptInputStream(InputStream data, boolean isClose, OutputStream out,
                                          byte[] keyBytes) throws IOException {
        Assert.notNull(data, "'data' must not null.");
        Assert.notNull(out, "'out' must not null.");
        Assert.notNull(keyBytes, "'keyBytes' must not null.");
        Key key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);
        try (out) {
            // 获取Cipher对象
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            // 设置加密模式
            cipher.init(Cipher.DECRYPT_MODE, key);
            // 创建解密输入流
            CipherInputStream cipherInputStream = new CipherInputStream(data, cipher);
            // 解密数据流
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = cipherInputStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            cipherInputStream.close();
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            throw new RuntimeException(e);
        } finally {
            if (isClose) {
                data.close();
            }
        }
    }


    /**
     * 根据密钥加密字节数组.
     *
     * @param keyBytes  密钥字节数组
     * @param dataBytes 待加密的字节数组
     * @return 经过Base64编码的 加密后的 字节数组
     */
    public static byte[] encryptByteArray(byte[] keyBytes, byte[] dataBytes) {
        // 获取密钥对象
        Key key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);

        try {
            // 获取Cipher对象
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);

            // 设置加密模式
            cipher.init(Cipher.ENCRYPT_MODE, key);

            // 执行加密并返回返回通过Base64编码的字节数组
            return Base64.getEncoder().encode(cipher.doFinal(dataBytes));
        } catch (IllegalBlockSizeException | NoSuchPaddingException
                 | InvalidKeyException | NoSuchAlgorithmException | BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据密钥加密字节数组.
     *
     * @param keyStrBase64 密钥字符串 经过Base64加密
     * @param dataBytes    待加密的字节数组
     * @return 经过Base64编码的 加密后的 字节数组
     * @see #encryptByteArray(byte[], byte[])
     */
    public static byte[] encryptByteArray(String keyStrBase64, byte[] dataBytes) {
        // 对key字符串 进行Base64解密
        byte[] keyBytes = Base64.getDecoder().decode(keyStrBase64);

        // 加密并返回
        return encryptByteArray(keyBytes, dataBytes);
    }

    /**
     * 根据密钥加密字节数组.
     *
     * @param keyFile   密钥文件 文件内容经过Base64加密
     * @param dataBytes 待加密数据字节数组
     * @return 经过Base64编码的 加密后的 字节数组
     * @throws IOException 文件IO操作异常
     * @see #encryptByteArray(byte[], byte[])
     */
    public static byte[] encryptByteArray(File keyFile, byte[] dataBytes) throws IOException {

        // 获取key字节数组-Base64编码格式
        byte[] keyBytesBase64 = Files.readAllBytes(keyFile.toPath());

        // 对key字符数组进行Base64解密
        byte[] keyBytes = Base64.getDecoder().decode(keyBytesBase64);

        // 加密并返回
        return encryptByteArray(keyBytes, dataBytes);
    }

    /**
     * 根据密钥加密文件.
     *
     * @param keyBytes 密钥字节数组
     * @param dataFile 待加密文件
     * @return 经过Base64编码的 加密后的 字节数组
     * @throws IOException 文件IO操作异常
     * @see #encryptByteArray(byte[], byte[])
     */
    public static byte[] encryptFile(byte[] keyBytes, File dataFile) throws IOException {
        // 获取待加密文件字节数组
        byte[] dataFileBytes = Files.readAllBytes(dataFile.toPath());

        // 加密并返回
        return encryptByteArray(keyBytes, dataFileBytes);
    }

    /**
     * 根据密钥加密文件.
     *
     * @param keyFile  密钥文件 文件内容经过Base64加密
     * @param dataFile 待加密文件
     * @return 经过Base64编码的 加密后的 字节数组
     * @throws IOException 文件IO操作异常
     * @see #encryptFile(byte[], File)
     */
    public static byte[] encryptFile(File keyFile, File dataFile) throws IOException {
        // 获取key字节数组-Base64编码格式
        byte[] keyBytesBase64 = Files.readAllBytes(keyFile.toPath());


        // 对key字符数组进行Base64解密
        byte[] keyBytes = Base64.getDecoder().decode(keyBytesBase64);

        // 加密并返回
        return encryptFile(keyBytes, dataFile);
    }

    /**
     * 根据密钥加密文件.
     *
     * @param keyFile      密钥文件 文件内容经过Base64加密
     * @param dataFile     待加密文件
     * @param encrptedFile 已加密文件 文件内容经过Base64加密
     * @throws IOException 文件IO流操作异常
     * @see #encryptFile(File, File)
     */
    public static void encryptFile(File keyFile, File dataFile, File encrptedFile)
        throws IOException {
        // 父目录不存在创建
        if (!encrptedFile.getParentFile().exists()) {
            encrptedFile.getParentFile().mkdirs();
        }

        // 加密文件获取加密后字节数组
        byte[] encryptedFileBytes = encryptFile(keyFile, dataFile);

        // 构建流输出到目标文件
        FileOutputStream fileOutputStream = new FileOutputStream(encrptedFile);
        fileOutputStream.write(encryptedFileBytes);

        // 释放流
        fileOutputStream.close();
    }


    /**
     * 根据密钥解密字节数组.
     *
     * @param keyBytes        密钥字节数组
     * @param dataBytesBase64 待解密数据字节数组 Base64加密格式
     * @return 解密后的字节数组
     */
    public static byte[] decryptByteArray(byte[] keyBytes, byte[] dataBytesBase64) {
        // 获取密钥对象
        Key key = new SecretKeySpec(keyBytes, KEY_ALGORITHM);

        try {
            // 获取Cipher对象
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);

            // 设置解密模式
            cipher.init(Cipher.DECRYPT_MODE, key);

            // Base64解密 获取AES加密后的加密数据数组
            byte[] dataBytes = Base64.getDecoder().decode(dataBytesBase64);

            // 执行解密并返回
            return cipher.doFinal(dataBytes);
        } catch (IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException
                 | InvalidKeyException | NoSuchPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 根据密钥解密字节数组.
     *
     * @param keyStrBase64    密钥字符串 Base64加密格式
     * @param dataBytesBase64 待解密数据字节数组 Base64加密格式
     * @return 解密后的字节数组
     * @see #decryptByteArray(byte[], byte[])
     */
    public static byte[] decryptByteArray(String keyStrBase64, byte[] dataBytesBase64) {
        // 对key字符串 进行Base64解密
        byte[] keyBytes = Base64.getDecoder().decode(keyStrBase64);
        return decryptByteArray(keyBytes, dataBytesBase64);
    }

    /**
     * 根据密钥解密文件.
     *
     * @param keyBytes 密钥字节数组
     * @param dataFile 待解密文件 文件内容经过Base64加密
     * @return 解密后的字节数组
     * @throws IOException 文件IO操作异常
     * @see #decryptByteArray(byte[], byte[])
     */
    public static byte[] decryptFile(byte[] keyBytes, File dataFile) throws IOException {
        // 获取文件内容字节数组 Base64加密格式
        byte[] dataBytesBase64 = Files.readAllBytes(dataFile.toPath());

        // 解密并返回
        return decryptByteArray(keyBytes, dataBytesBase64);
    }

    /**
     * 根据密钥解密文件.
     *
     * @param keyFile  密钥文件 文件内容经过Base64加密
     * @param dataFile 待解密文件  文件内容经过Base64加密
     * @return 解密后的字节数组
     * @throws IOException 文件IO操作异常
     * @see #decryptFile(byte[], File)
     */
    public static byte[] decryptFile(File keyFile, File dataFile) throws IOException {
        // 获取密钥文件内容 Base64加密格式
        byte[] keyBytesBase64 = Files.readAllBytes(keyFile.toPath());

        // 获取到原密钥文件内容
        byte[] keyBytes = Base64.getDecoder().decode(keyBytesBase64);

        // 解密并返回
        return decryptFile(keyBytes, dataFile);
    }

    /**
     * 根据密钥解密文件.
     *
     * @param keyFile      密钥文件 文件内容经过Base64加密
     * @param dataFile     待解密文件 文件内容经过Base64加密
     * @param decrptedFile 解密后的文件
     * @throws IOException 文件IO操作异常
     * @see #decryptFile(File, File)
     */
    public static void decryptFile(File keyFile, File dataFile, File decrptedFile)
        throws IOException {
        // 文件父目录不存在创建
        if (!decrptedFile.getParentFile().exists()) {
            decrptedFile.getParentFile().mkdirs();
        }

        // 解密文件获取解密后字节数组
        byte[] decryptBytes = decryptFile(keyFile, dataFile);

        // 构建流输入到文件
        FileOutputStream fileOutputStream = new FileOutputStream(decrptedFile);
        fileOutputStream.write(decryptBytes);

        // 释放流
        fileOutputStream.close();
    }

}
