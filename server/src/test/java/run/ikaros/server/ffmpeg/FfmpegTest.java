package run.ikaros.server.ffmpeg;

import static org.bytedeco.ffmpeg.global.avformat.av_dump_format;
import static org.bytedeco.ffmpeg.global.avformat.avformat_close_input;
import static org.bytedeco.ffmpeg.global.avformat.avformat_find_stream_info;
import static org.bytedeco.ffmpeg.global.avformat.avformat_open_input;
import static org.bytedeco.ffmpeg.global.avutil.av_strerror;

import org.bytedeco.ffmpeg.avformat.AVFormatContext;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.PointerPointer;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class FfmpegTest {

    static void check(int err) {
        if (err < 0) {
            BytePointer e = new BytePointer(512);
            av_strerror(err, e, 512);
            throw new RuntimeException(
                e.getString().substring(0, (int) BytePointer.strlen(e)) + ":" + err);
        }
    }

    @Test
    @Disabled
    void getVideoInfo() {
        String fileName = System.getenv("IKAROS_FFMPEG_TEST_VIDEO_URL");
        AVFormatContext inputFormatContext = new AVFormatContext(null);
        try {
            check(avformat_open_input(inputFormatContext, fileName, null, null));
            check(avformat_find_stream_info(inputFormatContext, (PointerPointer) null));
            av_dump_format(inputFormatContext, 0, fileName, 0);
        } finally {
            avformat_close_input(inputFormatContext);
        }
    }


}
