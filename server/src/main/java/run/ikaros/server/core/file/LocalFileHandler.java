package run.ikaros.server.core.file;

import static run.ikaros.api.core.file.FileConst.DEFAULT_FOLDER_ID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.File;
import run.ikaros.api.core.file.FileConst;
import run.ikaros.api.core.file.FileHandler;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.api.store.enums.FilePlace;
import run.ikaros.server.infra.properties.IkarosProperties;
import run.ikaros.server.infra.utils.FileUtils;
import run.ikaros.server.store.repository.FileRepository;

@Slf4j
@Component
public class LocalFileHandler implements FileHandler {
    private final IkarosProperties ikarosProp;
    private final FileRepository fileRepository;

    public LocalFileHandler(IkarosProperties ikarosProp, FileRepository fileRepository) {
        this.ikarosProp = ikarosProp;
        this.fileRepository = fileRepository;
    }

    @Override
    public String policy() {
        return FileConst.POLICY_LOCAL;
    }

    @Override
    public Mono<File> upload(UploadContext context) {
        FilePart filepart = context.filepart();
        LocalDateTime uploadTime = LocalDateTime.now();
        // format: [/upload/yyyy/MM/dd/HH/UUID.postfix].
        Path uploadPath = ikarosProp.getWorkDir()
            .resolve(FileConst.LOCAL_UPLOAD_DIR_NAME)
            .resolve(String.valueOf(uploadTime.getYear()))
            .resolve(String.valueOf(uploadTime.getMonthValue()))
            .resolve(String.valueOf(uploadTime.getDayOfMonth()))
            .resolve(String.valueOf(uploadTime.getHour()))
            .resolve(UUID.randomUUID().toString().replace("-", "")
                + "." + FileUtils.parseFilePostfix(filepart.filename()));
        try {
            // init parent folders
            Files.createDirectories(uploadPath.getParent());
        } catch (IOException e) {
            throw Exceptions.propagate(e);
        }
        // upload
        return filepart.transferTo(uploadPath.toFile())
            .doOnSuccess(unused -> log.debug("Upload file {} to dest path: {}", filepart.filename(),
                uploadPath))
            .then(Mono.just(FileEntity.builder()
                .folderId(DEFAULT_FOLDER_ID)
                .place(FilePlace.valueOf(context.policy()))
                .type(FileUtils.parseTypeByPostfix(FileUtils.parseFilePostfix(filepart.filename())))
                .url(uploadPath.toString().replace(ikarosProp.getWorkDir().toString(), "")
                    .replace(java.io.File.separatorChar, '/'))
                .name(filepart.filename())
                .originalPath(uploadPath.toString())
                .originalName(filepart.filename())
                .size(filepart.headers().getContentLength())
                .build()))
                .flatMap(fileRepository::save)
            .map(File::new);
    }

    @Override
    public Mono<File> delete(File file) {
        return Mono.justOrEmpty(file)
            .flatMap(file1 -> Mono.just(file1.entity()))
            .flatMap(entity -> fileRepository.delete(entity).then(Mono.just(entity)))
            .flatMap(entity -> Mono.just(entity.getOriginalPath()))
            .flatMap(path -> Mono.just(new java.io.File(path)))
            .flatMap(file1 -> Mono.just(file1.toPath()))
            .flatMap(path -> {
                try {
                    Files.delete(path);
                    log.debug("Delete file {} in path: {}", path.getFileName(), path);
                    return Mono.just(path);
                } catch (IOException e) {
                    return Mono.error(new RuntimeException(e));
                }
            })
            .then(Mono.just(file));
    }


}
