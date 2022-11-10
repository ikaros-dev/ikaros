package run.ikaros.server.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.exceptions.RegexMatchingException;

/**
 * @author li-guohao
 */
public class RegexUtils {

    @Nonnull
    public static String getFilePostfix(@Nonnull String fileName) {
        AssertUtils.notBlank(fileName, "fileName");
        Set<String> strSet = new HashSet<>();
        Matcher tagMatcher = Pattern.compile(RegexConst.FILE_POSTFIX).matcher(fileName);
        while (tagMatcher.find()) {
            strSet.add(tagMatcher.group());
        }
        return strSet.stream()
            .findFirst()
            .orElseThrow(() -> new RegexMatchingException("file postfix matching exception"));
    }

    @Nonnull
    public static Set<String> getFileTag(@Nonnull String fileName) {
        AssertUtils.notBlank(fileName, "fileName");
        Set<String> strSet = new HashSet<>();
        Matcher tagMatcher = Pattern.compile(RegexConst.FILE_NAME_TAG).matcher(fileName);
        while (tagMatcher.find()) {
            strSet.add(tagMatcher.group());
        }
        return strSet.stream()
            .map(postfix -> postfix.replace("[", "")
                .replace("]", ""))
            .filter(tag -> {
                String seqStr = String.valueOf(getFileNameTagEpSeq(fileName));
                if (seqStr.length() == 1) {
                    seqStr = "0" + seqStr;
                }
                return !tag.equalsIgnoreCase(seqStr);
            })
            .collect(Collectors.toSet());
    }

    @Nonnull
    public static Long getFileNameTagEpSeq(@Nonnull String fileName) {
        AssertUtils.notBlank(fileName, "fileName");
        Set<String> strSet = new HashSet<>();
        Matcher tagMatcher =
            Pattern.compile(RegexConst.FILE_NAME_TAG_EPISODE_SEQUENCE).matcher(fileName);
        while (tagMatcher.find()) {
            strSet.add(tagMatcher.group());
        }
        return strSet.stream()
            .map(postfix -> postfix.replace("[", "")
                .replace("]", ""))
            .flatMap((Function<String, Stream<Long>>) s -> {
                try {
                    return Stream.of(Long.parseLong(s));
                } catch (NumberFormatException numberFormatException) {
                    throw new RegexMatchingException(
                        "file name tag episode seq matching exception , file name: " + fileName);
                }
            }).findFirst().orElseThrow(() -> new RegexMatchingException(
                "file name tag episode seq matching exception, file name: " + fileName));
    }

}
