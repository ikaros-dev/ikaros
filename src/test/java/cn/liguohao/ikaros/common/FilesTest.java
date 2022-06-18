package cn.liguohao.ikaros.common;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author li-guohao
 * @date 2022/06/18
 */
class FilesTest {
    public static void main(String[] args) throws IOException {
        String originDirPath = "C:\\Pictures\\test\\from";
        String destDirPath = "C:\\Pictures\\test\\to";
        String fileName = "avator.jpeg";
        String originFilePath = originDirPath + File.separator + fileName;
        String destFilePath = destDirPath + File.separator + fileName;

        File originalFile = new File(originFilePath);
        if (!originalFile.exists()) {
            throw new IllegalArgumentException("original file not exist");
        }

        byte[] bytes = Files.readAllBytes(Path.of(originalFile.toURI()));
        Files.write(Path.of(new File(destFilePath).toURI()), bytes);
    }
}