package run.ikaros.server.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import run.ikaros.server.utils.SubtitleUtils;

/**
 * @author guohao
 * @date 2022/10/16
 */
@Disabled
class SubtitleUtilsTest {

    @Test
    void ass2vtt() throws IOException {
        File assFile = new File("C:\\Develop\\test-resource\\To Love-Ru Trouble "
            + "Darkness - 01 (BD 1280x720 AVC AACx2).SumiSora&CASO.sc.ass");

        String vttStr = SubtitleUtils.ass2vtt(assFile);

        FileWriter fileWriter = new FileWriter(new File("C:\\\\Develop\\\\test-resource"
            + "\\\\To.sc.webvtt"));
        fileWriter.write(vttStr);
    }
}