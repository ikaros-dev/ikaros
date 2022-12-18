package run.ikaros.server.utils;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.ikaros.server.constants.RegexConst;

import java.util.List;
import java.util.Set;

/**
 * @author li-guohao
 */
class RegexUtilsTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegexUtilsTest.class);

    final String fileName = "[VCB-Studio] K-ON! [01][Ma10p_1080p][x265_flac_2aac].mkv";
    final String title = "手工少女!! / Do It Yourself!! - 06 ";

    @Test
    void getFilePostfix() {
        String filePostfix = RegexUtils.getFilePostfix(fileName);
        Assertions.assertEquals(".mkv", filePostfix);
    }

    @Test
    void getFileTag() {
        List<String> tagSet = RegexUtils.getFileTag(fileName);
        Assertions.assertFalse(tagSet.isEmpty());
        Assertions.assertEquals(3, tagSet.size());
        Assertions.assertTrue(tagSet.contains("VCB-Studio"));
        Assertions.assertFalse(tagSet.contains("01"));
        Assertions.assertTrue(tagSet.contains("Ma10p_1080p"));
        Assertions.assertTrue(tagSet.contains("x265_flac_2aac"));
    }

    @Test
    void getFileNameTagEpSeq() {
        String episodeFileName = "[Nekomoe kissaten&LoliHouse] Urusei Yatsura 2022 "
            + "- 08 [WebRip 1080p HEVC-10bit AAC ASSx2].mkv";
        Long seq = RegexUtils.getFileNameTagEpSeq(episodeFileName);
        Assertions.assertEquals(8L, seq);
    }

    @Test
    void getMatchingStr() {
        final String regex = "[\\u2E80-\\u9FFF]";
        String matchingStr = RegexUtils.getMatchingStr(title, regex);
        LOGGER.info("matching str: {}", matchingStr);
        Assertions.assertNotNull(matchingStr);
    }

    @Test
    void getMatchingEnglishStr() {
        String str = "[Eternal][Harem Camp!][06][GB][720P][Premium].mp4";
        String matchingEnglishStr = RegexUtils.getMatchingEnglishStrWithoutTag(str);
        LOGGER.info("matching english str: {}", matchingEnglishStr);
        Assertions.assertNotNull(matchingEnglishStr);
    }

    @Test
    void getMatchingChineseStr() {
        String matchingChineseStr = RegexUtils.getMatchingChineseStr(title);
        LOGGER.info("matching chinese str: {}", matchingChineseStr);
        Assertions.assertNotNull(matchingChineseStr);
    }
}