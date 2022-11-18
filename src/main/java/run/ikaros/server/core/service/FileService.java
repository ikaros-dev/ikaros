package run.ikaros.server.core.service;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.entity.FileEntity;
import run.ikaros.server.exceptions.FileNameMatchingException;
import run.ikaros.server.params.SearchFilesParams;
import run.ikaros.server.result.PagingWrap;
import run.ikaros.server.utils.AssertUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author li-guohao
 */
public interface FileService extends CrudService<FileEntity, Long> {

    @Nonnull
    @Transactional
    FileEntity upload(@Nonnull String originalFilename, @Nonnull byte[] bytes);

    @Nonnull
    FileEntity findById(@Nonnull Long fileId);

    @Transactional
    void delete(@Nonnull Long fileId);

    @Nonnull
    @Transactional
    FileEntity update(@Nonnull FileEntity fileEntity);

    @Nonnull
    @Transactional
    FileEntity update(@Nonnull Long fileId, @Nonnull MultipartFile multipartFile)
        throws IOException;

    @Nonnull
    PagingWrap<FileEntity> findFilesByPagingAndCondition(
        @Nonnull SearchFilesParams searchFilesParams);

    @Nonnull
    Set<String> findTypes();

    @Nonnull
    Set<String> findPlaces();

    @Transactional
    void deleteInBatch(@Nonnull Set<Long> ids);

    @Nonnull
    @Transactional
    FileEntity updateNameById(@Nonnull String name, @Nonnull Long id);

    @Transactional
    void receiveAndHandleChunkFile(@Nonnull String unique, @Nonnull String uploadLength,
                                   @Nonnull String uploadOffset, @Nonnull String uploadName,
                                   @Nonnull byte[] bytes) throws IOException;


    @Nonnull
    default Long getEpisodeSeqFromName(@Nonnull String name) {
        AssertUtils.notBlank(name, "file name");
        final String originalName = name;
        Set<String> seqSet = new HashSet<>();
        // 目前的匹配逻辑：先筛选中括号内的
        Matcher tagEpSeqMatcher =
            Pattern.compile(RegexConst.FILE_NAME_TAG_EPISODE_SEQUENCE).matcher(name);
        while (tagEpSeqMatcher.find()) {
            seqSet.add(tagEpSeqMatcher.group());
        }

        // 如果中括号里找不到，则先去掉所有的中括号
        name = name.replaceAll(RegexConst.FILE_NAME_TAG, "");

        // 去掉文件后缀
        name = name.replaceAll(RegexConst.FILE_POSTFIX, "");

        // 匹配一位或者两位的数字
        Matcher numEpSeqMatcher = Pattern.compile(RegexConst.NUMBER_EPISODE_SEQUENCE).matcher(name);
        while (numEpSeqMatcher.find()) {
            seqSet.add(numEpSeqMatcher.group());
        }

        Optional<String> firstSeqStrOptional = seqSet.stream().findFirst();
        if (firstSeqStrOptional.isEmpty()) {
            throw new FileNameMatchingException(
                "not found episode seq by file name: " + originalName);
        }

        String firstSeqStr = firstSeqStrOptional.get();

        if (firstSeqStr.startsWith("[")) {
            firstSeqStr = firstSeqStr.replace("[", "")
                .replace("]", "");
        }

        try {
            return Long.parseLong(firstSeqStr);
        } catch (NumberFormatException numberFormatException) {
            throw new FileNameMatchingException("special file name, ikaros can't "
                + "matching automatically, please config it manually, file name: " + originalName);
        }
    }

    @Nonnull
    FileEntity create(@Nonnull FileEntity fileEntity);
}
