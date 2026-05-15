package run.ikaros.server.infra.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Base64;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class AesEncryptUtilsTest {

    @Test
    @Disabled
    void encryptInputStream() throws IOException {
        String fileName = "2023-06-15 09-56-02.mp4";

        byte[] keyByteArray = AesEncryptUtils.generateKeyByteArray();
        String key = new String(Base64.getDecoder().decode(keyByteArray));
        assertThat(key).isNotBlank();

        File file = new File("C:\\Users\\li-guohao\\Pictures\\tests\\original\\" + fileName);
        FileInputStream data = new FileInputStream(file);
        File encryptFile = new File("C:\\Users\\li-guohao\\Pictures\\tests\\encrypt\\" + fileName);
        FileOutputStream out = new FileOutputStream(encryptFile);
        AesEncryptUtils.encryptInputStream(data, true, out, keyByteArray);

        FileInputStream inputStream = new FileInputStream(encryptFile);
        File newFile = new File("C:\\Users\\li-guohao\\Pictures\\tests\\decrypt\\" + fileName);
        FileOutputStream outputStream = new FileOutputStream(newFile);
        AesEncryptUtils.decryptInputStream(inputStream, true, outputStream, keyByteArray);
    }
}