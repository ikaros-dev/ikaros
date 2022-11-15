package run.ikaros.server.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.ikaros.server.constants.RegexConst;
import run.ikaros.server.exceptions.RegexMatchingException;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author li-guohao
 */
public class RegexUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegexUtils.class);

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
        final String originalFileName = fileName;

        // matching file tag that is seq
        Matcher matcher =
            Pattern.compile(RegexConst.FILE_NAME_TAG_EPISODE_SEQUENCE_WITH_BRACKETS)
                .matcher(fileName);
        while (matcher.find()) {
            strSet.add(matcher.group());
        }

        // remove file tag if exist
        fileName = fileName.replaceAll(RegexConst.FILE_NAME_TAG, "");
        // remove file postfix if exist
        fileName = fileName.replaceAll(RegexConst.FILE_POSTFIX, "");

        // matching seq
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
                        "file name tag episode seq matching exception , file name: "
                            + originalFileName);
                }
            }).findFirst().orElseThrow(() -> new RegexMatchingException(
                "file name tag episode seq matching exception, file name: " + originalFileName));
    }

    @Nonnull
    public static String getMatchingStr(@Nonnull String originalStr, @Nonnull String regex) {
        AssertUtils.notBlank(originalStr, "originalStr");
        AssertUtils.notBlank(regex, "regex");
        List<String> strList = new ArrayList<>();
        Matcher tagMatcher =
            Pattern.compile(regex).matcher(originalStr);
        while (tagMatcher.find()) {
            strList.add(tagMatcher.group());
        }
        if (strList.isEmpty()) {
            throw new RegexMatchingException(
                "[" + regex + "] match fail for originalStr=" + originalStr);
        }
        StringBuilder sb = new StringBuilder();
        for (String str : strList) {
            sb.append(str);
        }

        return sb.toString().trim();
    }

    @Nonnull
    public static String getMatchingEnglishStr(String str) {
        AssertUtils.notBlank(str, "str");
        final String regex = "[A-Za-z\\s]";
        return getMatchingStr(str, regex);
    }

    @Nonnull
    public static String getMatchingEnglishStrWithoutTag(String str) {
        AssertUtils.notBlank(str, "str");
        final String originalStr = str;
        str = str.replaceAll(RegexConst.FILE_NAME_TAG, "");
        if (StringUtils.isBlank(str)) {
            LOGGER.warn("str is blank after remove file tag, originalStr={}", originalStr);
            return str;
        }
        final String regex = "[A-Za-z\\s]";
        return getMatchingStr(str, regex);
    }

    @Nonnull
    public static String getMatchingChineseStr(String str) {
        AssertUtils.notBlank(str, "str");
        final String regex = "[\\u2E80-\\u9FFF]";
        return getMatchingStr(str, regex);
    }

    @Nonnull
    public static String getMatchingChineseStrWithoutTag(String str) {
        AssertUtils.notBlank(str, "str");
        final String originalStr = str;
        str = str.replaceAll(RegexConst.FILE_NAME_TAG, "");
        if (StringUtils.isBlank(str)) {
            LOGGER.warn("str is blank after remove file tag, originalStr={}", originalStr);
            return str;
        }
        final String regex = "[\\u2E80-\\u9FFF]";
        return getMatchingStr(str, regex);
    }

}
