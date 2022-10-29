package run.ikaros.server.constants;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * @author li-guohao
 */
class RegexConstTest {

    @Test
    void regex() {
        final String seqRegex = RegexConst.FILE_NAME_EPISODE_SEQUENCE;
        final String tagRegex = RegexConst.FILE_NAME_TAG;
        String fileName = "[VCB-Studio] [02] [3] Sora no Otoshimono II [02][Hi10p_1080p][x264_2flac].mkv";
        Matcher matcher = Pattern.compile(seqRegex).matcher(fileName);

        List<String> strList = new ArrayList<>();
        while (matcher.find()) {
            strList.add(matcher.group());
        }
        System.out.println(strList);
    }

}