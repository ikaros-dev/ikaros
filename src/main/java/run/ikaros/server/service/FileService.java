package run.ikaros.server.service;

import java.io.IOException;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import org.springframework.web.multipart.MultipartFile;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.params.SearchFilesParams;
import run.ikaros.server.result.PagingWrap;
import run.ikaros.server.service.base.CrudService;

/**
 * @author li-guohao
 */
public interface FileService extends CrudService<FileEntity, Long> {

    @Nonnull
    @Transactional(rollbackOn = Exception.class)
    FileEntity upload(@Nonnull String originalFilename, @Nonnull byte[] bytes);

    @Nonnull
    FileEntity findById(@Nonnull Long fileId);

    @Transactional(rollbackOn = Exception.class)
    void delete(@Nonnull Long fileId);

    @Nonnull
    @Transactional(rollbackOn = Exception.class)
    FileEntity update(@Nonnull FileEntity fileEntity);

    @Nonnull
    @Transactional(rollbackOn = Exception.class)
    FileEntity update(@Nonnull Long fileId, @Nonnull MultipartFile multipartFile)
        throws IOException;

    @Nonnull
    PagingWrap<FileEntity> findFilesByPagingAndCondition(
        @Nonnull SearchFilesParams searchFilesParams);

    @Nonnull
    Set<String> findTypes();

    @Nonnull
    Set<String> findPlaces();

    @Transactional(rollbackOn = Exception.class)
    void deleteInBatch(@Nonnull Set<Long> ids);

    @Nonnull
    @Transactional(rollbackOn = Exception.class)
    FileEntity updateNameById(@Nonnull String name, @Nonnull Long id);

    @Transactional(rollbackOn = Exception.class)
    void receiveAndHandleChunkFile(@Nonnull String unique, @Nonnull String uploadLength,
                                   @Nonnull String uploadOffset, @Nonnull String uploadName,
                                   @Nonnull byte[] bytes) throws IOException;
}
