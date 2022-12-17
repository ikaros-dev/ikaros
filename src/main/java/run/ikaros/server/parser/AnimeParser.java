package run.ikaros.server.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.core.tripartite.bgmtv.service.BgmTvService;
import run.ikaros.server.exceptions.RegexMatchingException;
import run.ikaros.server.utils.AssertUtils;
import run.ikaros.server.utils.JsonUtils;
import run.ikaros.server.utils.RegexUtils;
import run.ikaros.server.utils.StringUtils;

import jakarta.annotation.Nonnull;
import java.util.List;

public class AnimeParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(AnimeParser.class);

    public static AnimeEpisodeInfo parseInfoByEpisodeFileName(@Nonnull String episodeFileName) {
        AssertUtils.notBlank(episodeFileName, "episodeFileName");
        AnimeEpisodeInfo animeEpisodeInfo = new AnimeEpisodeInfo();
        animeEpisodeInfo.setName(episodeFileName);

        String fileName = episodeFileName
            .replaceAll(RegexConst.FILE_POSTFIX, "")
            .replace("【", "[")
            .replace("】", "]")
            .trim();

        List<String> fileTagList = RegexUtils.getFileTag(fileName);
        animeEpisodeInfo.setTags(JsonUtils.obj2Json(fileTagList.toArray()));


        String fileNameWithoutTag = fileName.replaceAll(RegexConst.FILE_NAME_TAG, "").trim();
        if (StringUtils.isBlank(fileNameWithoutTag)) {
            // 文件名全部都被中括号包裹起来了，需要解析所有的Tag，找到可能是文件名的并获取对应的值
            if (fileTagList.size() > 2) {
                fileNameWithoutTag = fileTagList.get(1);
            }


        }

        try {
            String matchingChineseStr = RegexUtils.getMatchingChineseStr(fileNameWithoutTag);
            animeEpisodeInfo.setChineseTitle(matchingChineseStr);
        } catch (RegexMatchingException regexMatchingException) {
            LOGGER.warn("matching chinese fail for filename={}, exception msg={}",
                episodeFileName, regexMatchingException.getMessage());
        }

        try {
            String matchingEnglishStr = RegexUtils.getMatchingEnglishStr(fileNameWithoutTag);
            matchingEnglishStr = matchingEnglishStr
                .replace(" S ", "")
                .replace(" S", "")
                .replace("S ", "");
            animeEpisodeInfo.setRomajiTitle(matchingEnglishStr);
        } catch (RegexMatchingException regexMatchingException) {
            LOGGER.warn("matching english fail for filename={}, exception msg={}",
                episodeFileName, regexMatchingException.getMessage());
        }

        try {
            String matchingSeqStr = RegexUtils
                .getMatchingStr(fileNameWithoutTag, RegexConst.NUMBER_EPISODE_SEQUENCE);
            Long seq = Long.parseLong(matchingSeqStr);
            animeEpisodeInfo.setSequence(seq);
        } catch (RegexMatchingException | NumberFormatException exception) {
            LOGGER.warn("matching english fail for filename={}, exception msg={}",
                episodeFileName, exception.getMessage());
        }

        if (animeEpisodeInfo.getSequence() == null) {
            for (String tag : fileTagList) {
                try {
                    String matchingSeqStr = RegexUtils
                        .getMatchingStr(tag, RegexConst.NUMBER_EPISODE_SEQUENCE);
                    Long seq = Long.parseLong(matchingSeqStr);
                    animeEpisodeInfo.setSequence(seq);
                } catch (RegexMatchingException | NumberFormatException exception) {
                    LOGGER.warn("matching seq in tag fail for filename={}, exception msg={}",
                        episodeFileName, exception.getMessage());
                }
            }
        }

        return animeEpisodeInfo;
    }

}
