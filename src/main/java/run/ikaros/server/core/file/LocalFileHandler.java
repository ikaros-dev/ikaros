package run.ikaros.server.core.file;

import static run.ikaros.server.core.file.FileConst.DEFAULT_FOLDER_ID;

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
import run.ikaros.server.infra.properties.IkarosProperties;
import run.ikaros.server.infra.utils.FileUtils;
import run.ikaros.server.store.entity.FileEntity;
import run.ikaros.server.store.enums.FilePlace;
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
        filepart.transferTo(uploadPath.toFile()).block();
        log.debug("Upload file {} to dest path: {}", filepart.filename(), uploadPath);
        // save to db
        FileEntity fileEntity = FileEntity.builder()
            .folderId(DEFAULT_FOLDER_ID)
            .place(FilePlace.valueOf(context.policy()))
            .type(FileUtils.parseTypeByPostfix(FileUtils.parseFilePostfix(filepart.filename()))
                .toString())
            .url(uploadPath.toString().replace(ikarosProp.getWorkDir().toString(), "")
                .replace(java.io.File.separatorChar, '/'))
            .name(filepart.filename())
            .originalPath(uploadPath.toString())
            .originalName(filepart.filename())
            .size(filepart.headers().getContentLength())
            .build();
        return fileRepository.save(fileEntity)
            .map(File::new);
    }

    @Override
    public Mono<File> delete(File file) {
        return null;
    }


}
