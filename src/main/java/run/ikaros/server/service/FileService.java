package run.ikaros.server.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nonnull;
import javax.transaction.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.multipart.MultipartFile;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.exceptions.FileNameMatchingFailException;
import run.ikaros.server.params.SearchFilesParams;
import run.ikaros.server.result.PagingWrap;
import run.ikaros.server.service.base.CrudService;
import run.ikaros.server.utils.AssertUtils;

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


    @Nonnull
    default Long getEpisodeSeqFromName(@Nonnull String name) {
        AssertUtils.notBlank(name, "file name");
        Matcher matcher = Pattern.compile(RegexConst.FILE_NAME_EPISODE_SEQUENCE).matcher(name);
        List<String> seqList = new ArrayList<>();
        while (matcher.find()) {
            seqList.add(matcher.group());
        }

        if (seqList.isEmpty()) {
            throw new FileNameMatchingFailException("file name is: " + name);
        }

        String seqStr = seqList.get(0)
            .replace("[", "")
            .replace("]", "");
        return Long.parseLong(seqStr);
    }
}
