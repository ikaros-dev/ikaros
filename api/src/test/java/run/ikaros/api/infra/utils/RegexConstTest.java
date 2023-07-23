package run.ikaros.api.infra.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class RegexConstTest {
    @Test
    void testFileNameEpSeqWithBlank() {
        String fileName =
            "[ANi] Reign of the Seven Spellblades - 七魔剑支配天下 - 03 [1080P][Baha][WEB-DL][AAC AVC][CHT][MP4]";
        Set<String> strSet = new HashSet<>();

        Matcher matcher =
            Pattern.compile(RegexConst.FILE_NAME_EPISODE_SEQUENCE_WITH_BLANK)
                .matcher(fileName);
        while (matcher.find()) {
            strSet.add(matcher.group());
        }

        Assertions.assertThat(strSet).isNotEmpty();
    }
}