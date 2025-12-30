package run.ikaros.server.infra.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
public class DataBufferFileWriter {

    /**
     * 写入文件并返回
     * .
     */
    public static Mono<Void> writeFluxToFile(Flux<DataBuffer> dataBufferFlux, String filePath) {
        Path path = Paths.get(filePath);

        return DataBufferUtils.write(dataBufferFlux, path,
            StandardOpenOption.CREATE,
            StandardOpenOption.WRITE,
            StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 自定义写入逻辑.
     */
    public static Mono<Void> writeFluxToFileWithProgress(Flux<DataBuffer> dataBufferFlux,
                                                         String filePath) {
        Path path = Paths.get(filePath);

        return Mono.using(
            () -> AsynchronousFileChannel.open(
                path,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
            ),
            channel -> dataBufferFlux
                .index() // 添加索引跟踪进度
                .concatMap(tuple -> {
                    long index = tuple.getT1();
                    DataBuffer buffer = tuple.getT2();
                    ByteBuffer byteBuffer = buffer.asByteBuffer();

                    // 计算写入位置
                    long position = index * 4096; // 假设每个buffer大小为4KB

                    return DataBufferUtils.write(
                            Flux.just(buffer),
                            channel,
                            position
                        )
                        .doOnNext(written -> {
                            log.debug("Written buffer {}, position: {}/{}", index, index, position);
                            //System.out.printf
                            // ("Written buffer %d, position: %d%n", index, position);
                        });
                })
                .then(),
            channel -> {
                try {
                    channel.close();
                } catch (Exception e) {
                    // 处理关闭异常
                }
            }
        );
    }


    /**
     * 将目录打包成ZIP并返回
     * .
     */
    public static Flux<DataBuffer> zipDirectoryToStream(String sourceDirPath) {
        return Flux.create(sink -> {
            try {
                Path sourceDir = Paths.get(sourceDirPath);

                if (!Files.exists(sourceDir) || !Files.isDirectory(sourceDir)) {
                    sink.error(new IllegalArgumentException("目录不存在: " + sourceDirPath));
                    return;
                }

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos);
                AtomicLong totalFiles = new AtomicLong(0);
                AtomicLong processedFiles = new AtomicLong(0);

                // 首先统计文件总数
                try {
                    totalFiles.set(Files.walk(sourceDir)
                        .filter(path -> !Files.isDirectory(path))
                        .count());
                } catch (IOException e) {
                    sink.error(e);
                    return;
                }

                // 遍历目录并添加到ZIP
                try {
                    Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                            throws IOException {
                            try {
                                // 计算相对路径
                                Path relativePath = sourceDir.relativize(file);
                                String entryName = relativePath.toString();

                                // 处理路径分隔符
                                if (File.separatorChar != '/') {
                                    entryName = entryName.replace(File.separatorChar, '/');
                                }

                                // 创建ZIP条目
                                ZipEntry zipEntry = new ZipEntry(entryName);
                                zos.putNextEntry(zipEntry);

                                // 写入文件内容
                                Files.copy(file, zos);
                                zos.closeEntry();

                                // 更新进度
                                long processed = processedFiles.incrementAndGet();

                                log.info("已处理: {}/{}文件: {}",
                                    processed, totalFiles.get(), file.getFileName());
                                // System.out.printf("已处理: %d/%d 文件: %s%n",
                                //    processed, totalFiles.get(), file.getFileName());

                            } catch (Exception e) {
                                sink.error(e);
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult preVisitDirectory(Path dir,
                                                                 BasicFileAttributes attrs)
                            throws IOException {
                            if (!dir.equals(sourceDir)) {
                                Path relativePath = sourceDir.relativize(dir);
                                String entryName = relativePath.toString() + "/";

                                if (File.separatorChar != '/') {
                                    entryName = entryName.replace(File.separatorChar, '/');
                                }

                                ZipEntry zipEntry = new ZipEntry(entryName);
                                zos.putNextEntry(zipEntry);
                                zos.closeEntry();
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc)
                            throws IOException {
                            sink.error(exc);
                            return FileVisitResult.TERMINATE;
                        }
                    });

                    // 完成ZIP创建
                    zos.close();

                    // 将ZIP数据发送出去
                    byte[] zipData = baos.toByteArray();
                    DataBuffer buffer = new DefaultDataBufferFactory().wrap(zipData);
                    sink.next(buffer);
                    sink.complete();

                    log.info("目录压缩完成，总文件数: {}", totalFiles.get());
                    // System.out.println("目录压缩完成，总文件数: " + totalFiles.get());

                } catch (Exception e) {
                    sink.error(e);
                }

            } catch (Exception e) {
                sink.error(e);
            }
        });
    }
}