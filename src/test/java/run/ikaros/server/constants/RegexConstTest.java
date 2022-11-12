package run.ikaros.server.constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author li-guohao
 */
class RegexConstTest {

    @Test
    void fileNameTagEpSeqRegex() {
        Set<String> strSet = new HashSet<>();
        String fileName = "[VCB-Studio] K-ON! [01][Ma10p_1080p][x265_flac_2aac].mkv";
        Matcher tagMatcher =
            Pattern.compile(RegexConst.FILE_NAME_TAG_EPISODE_SEQUENCE).matcher(fileName);
        while (tagMatcher.find()) {
            strSet.add(tagMatcher.group());
        }
        Assertions.assertFalse(strSet.isEmpty());
        Assertions.assertEquals(1, strSet.size());
        Assertions.assertTrue(strSet.contains("[01]"));
    }


    @Test
    void fileTagRegex() {
        Set<String> strSet = new HashSet<>();
        String fileName = "[VCB-Studio] K-ON! [01][Ma10p_1080p][x265_flac_2aac].mkv";
        Matcher tagMatcher = Pattern.compile(RegexConst.FILE_NAME_TAG).matcher(fileName);
        while (tagMatcher.find()) {
            strSet.add(tagMatcher.group());
        }
        Assertions.assertFalse(strSet.isEmpty());
        Assertions.assertEquals(4, strSet.size());
        Assertions.assertTrue(strSet.contains("[VCB-Studio]"));
        Assertions.assertTrue(strSet.contains("[01]"));
        Assertions.assertTrue(strSet.contains("[Ma10p_1080p]"));
        Assertions.assertTrue(strSet.contains("[x265_flac_2aac]"));
    }

    @Test
    void filePostfixRegex() {
        Set<String> strSet = new HashSet<>();
        String fileName = "[VCB-Studio] K-ON! [01][Ma10p_1080p][x265_flac_2aac].mkv";
        Matcher tagMatcher = Pattern.compile(RegexConst.FILE_POSTFIX).matcher(fileName);
        while (tagMatcher.find()) {
            strSet.add(tagMatcher.group());
        }
        Assertions.assertFalse(strSet.isEmpty());
        Assertions.assertEquals(1, strSet.size());
        Assertions.assertTrue(strSet.contains(".mkv"));
    }

}