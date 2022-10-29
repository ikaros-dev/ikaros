package run.ikaros.server.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import run.ikaros.server.constants.RegexConst;

/**
 * @author li-guohao
 */
class RegexUtilsTest {

    String fileName = "[VCB-Studio] K-ON! [01][Ma10p_1080p][x265_flac_2aac].mkv";

    @Test
    void getFilePostfix() {
        String filePostfix = RegexUtils.getFilePostfix(fileName);
        Assertions.assertEquals(".mkv", filePostfix);
    }

    @Test
    void getFileTag() {
        Set<String> tagSet = RegexUtils.getFileTag(fileName);
        Assertions.assertFalse(tagSet.isEmpty());
        Assertions.assertEquals(3, tagSet.size());
        Assertions.assertTrue(tagSet.contains("VCB-Studio"));
        Assertions.assertFalse(tagSet.contains("01"));
        Assertions.assertTrue(tagSet.contains("Ma10p_1080p"));
        Assertions.assertTrue(tagSet.contains("x265_flac_2aac"));
    }

    @Test
    void getFileNameTagEpSeq() {
        Long seq = RegexUtils.getFileNameTagEpSeq(fileName);
        Assertions.assertEquals(01L, seq);
    }
}