package run.ikaros.server.file.utils;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import run.ikaros.server.file.constatns.FileConst;

/**
 * @author li-guohao
 */
public class FileUtils {

    static final Set<String> IMAGES =
        Arrays.stream(FileConst.Postfix.IMAGES).collect(Collectors.toSet());
    static final Set<String> DOCUMENTS =
        Arrays.stream(FileConst.Postfix.DOCUMENTS).collect(Collectors.toSet());
    static final Set<String> VIDEOS =
        Arrays.stream(FileConst.Postfix.VIDEOS).collect(Collectors.toSet());
    static final Set<String> VOICES =
        Arrays.stream(FileConst.Postfix.VOICES).collect(Collectors.toSet());

    private static final String BASE_UPLOAD_DIR_NAME = "upload";

}
