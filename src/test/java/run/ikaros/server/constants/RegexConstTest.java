package run.ikaros.server.constants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
        String fileName = "[VCB-Studio] K-ON! [01][Ma10p_1080p][ACC 1080p][x265_flac_2aac].mkv";
        Matcher tagMatcher = Pattern.compile(RegexConst.FILE_NAME_TAG).matcher(fileName);
        while (tagMatcher.find()) {
            strSet.add(tagMatcher.group());
        }
        Assertions.assertFalse(strSet.isEmpty());
        Assertions.assertEquals(5, strSet.size());
        Assertions.assertTrue(strSet.contains("[VCB-Studio]"));
        Assertions.assertTrue(strSet.contains("[01]"));
        Assertions.assertTrue(strSet.contains("[Ma10p_1080p]"));
        Assertions.assertTrue(strSet.contains("[x265_flac_2aac]"));
        Assertions.assertTrue(strSet.contains("[ACC 1080p]"));
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

    // @Test
    void brackets() {
        String fileName =
            "[ANi] 秋葉原冥途戰爭（僅限港澳台地區） - 06 [1080P][Bilibili][WEB-DL][AAC AVC][CHT CHS].mp4";
        Matcher matcher = Pattern.compile("（//w）").matcher(fileName);
        List<String> setList = new ArrayList<>();
        while (matcher.find()) {
            setList.add(matcher.group());
        }
        assertThat(setList).isNotEmpty();
    }

    @Test
    void seasonSeq() {
        String fileName =
            "[Lilith-Raws] Muv-Luv Alternative S02 "
                + "- 07 [Baha][WEB-DL][1080p][AVC AAC][CHT][MP4].mp4";
        Matcher matcher =
            Pattern.compile(RegexConst.NUMBER_SEASON_SEQUENCE_WITH_PREFIX).matcher(fileName);
        List<String> setList = new ArrayList<>();
        while (matcher.find()) {
            setList.add(matcher.group());
        }
        assertThat(setList).isNotEmpty();
    }

    @Test
    void year() {
        String year = "12310 2022 2013";
        Matcher matcher =
            Pattern.compile(RegexConst.YEAR).matcher(year);
        List<String> setList = new ArrayList<>();
        while (matcher.find()) {
            setList.add(matcher.group());
        }
        assertThat(setList).isNotEmpty();
    }
}