package run.ikaros.api.infra.utils;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import run.ikaros.api.infra.exception.RegexMatchingException;

public class RegexUtils {
    private static final Logger log = LoggerFactory.getLogger(RegexUtils.class);

    /**
     * Get file Postfix.
     */
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

    /**
     * Get file tag.
     */
    @Nonnull
    public static List<String> getFileTag(@Nonnull String fileName) {
        AssertUtils.notBlank(fileName, "fileName");
        if ("[]".equalsIgnoreCase(fileName)) {
            return List.of();
        }
        List<String> stringList = new ArrayList<>();
        Matcher tagMatcher = Pattern.compile(RegexConst.FILE_NAME_TAG).matcher(fileName);
        while (tagMatcher.find()) {
            stringList.add(tagMatcher.group());
        }
        return stringList.stream()
            .map(postfix -> postfix.replace("[", "")
                .replace("]", ""))
            .filter(tag -> {
                String seqStr = String.valueOf(getFileNameTagEpSeq(fileName));
                if (seqStr.length() == 1) {
                    seqStr = "0" + seqStr;
                }
                return !tag.equalsIgnoreCase(seqStr);
            })
            .collect(Collectors.toList());
    }

    /**
     * Get file name tag episode seq, such as: xxxxxxx[02]xxxxxx.mp4 => 02
     * .
     */
    @Nonnull
    public static Double getFileNameTagEpSeq(@Nonnull final String fileName) {
        AssertUtils.notBlank(fileName, "fileName");
        Set<String> strSet = new HashSet<>();
        if ("[]".equalsIgnoreCase(fileName)) {
            return -1D;
        }
        String originalFileName = fileName;

        // matching file tag that is seq
        Matcher matcher =
            Pattern.compile(RegexConst.FILE_NAME_TAG_EPISODE_SEQUENCE_WITH_BRACKETS)
                .matcher(fileName);
        while (matcher.find()) {
            strSet.add(matcher.group());
        }

        return strSet.stream()
            .map(postfix -> postfix.replace("[", "")
                .replace("]", ""))
            .flatMap((Function<String, Stream<Double>>) s -> {
                try {
                    return Stream.of(Double.parseDouble(s));
                } catch (NumberFormatException numberFormatException) {
                    throw new RegexMatchingException(
                        "file name tag episode seq matching exception , file name: "
                            + fileName);
                }
            }).findFirst().orElseThrow(() -> new RegexMatchingException(
                "file name tag episode seq matching exception, file name: " + fileName));
    }

    /**
     * Get episode seq by file name, such as: xxxx 04 xxxx.mp4 => 04 .
     */
    @Nonnull
    public static Double getFileNameBlankEpSeq(@Nonnull final String fileName) {
        AssertUtils.notBlank(fileName, "fileName");
        Set<String> strSet = new HashSet<>();

        Matcher matcher =
            Pattern.compile(RegexConst.FILE_NAME_EPISODE_SEQUENCE_WITH_BLANK)
                .matcher(fileName);
        while (matcher.find()) {
            strSet.add(matcher.group());
        }

        if (strSet.isEmpty()) {
            throw new RegexMatchingException(
                "file name episode seq matching exception , file name: "
                    + fileName);
        }

        return strSet.stream().findFirst()
            .map(String::trim)
            .map(Double::parseDouble)
            .orElse(null);
    }

    /**
     * Get episode seq by file name, such as: xxxx-04-xxxx.mp4 => 04 .
     */
    @Nonnull
    public static Double getFileNameHorizontalEpSeq(@Nonnull final String fileName) {
        AssertUtils.notBlank(fileName, "fileName");
        Set<String> strSet = new HashSet<>();

        Matcher matcher =
            Pattern.compile(RegexConst.FILE_NAME_EPISODE_SEQUENCE_WITH_HORIZONTAL)
                .matcher(fileName);
        while (matcher.find()) {
            strSet.add(matcher.group());
        }

        if (strSet.isEmpty()) {
            throw new RegexMatchingException(
                "file name episode seq matching exception , file name: "
                    + fileName);
        }

        return strSet.stream().findFirst()
            .map(str -> str.replace("-", ""))
            .map(Double::parseDouble)
            .orElse(null);
    }

    /**
     * Get episode seq by file name, such as: xxxx_04_xxxx.mp4 => 04 .
     */
    @Nonnull
    public static Double getFileNameUnderlineEpSeq(@Nonnull final String fileName) {
        AssertUtils.notBlank(fileName, "fileName");
        Set<String> strSet = new HashSet<>();

        Matcher matcher =
            Pattern.compile(RegexConst.FILE_NAME_EPISODE_SEQUENCE_WITH_UNDERLINE)
                .matcher(fileName);
        while (matcher.find()) {
            strSet.add(matcher.group());
        }

        if (strSet.isEmpty()) {
            throw new RegexMatchingException(
                "file name episode seq matching exception , file name: "
                    + fileName);
        }

        return strSet.stream().findFirst()
            .map(str -> str.replace("_", ""))
            .map(Double::parseDouble)
            .orElse(null);
    }

    /**
     * Get episode seq by file name and ep integrally, such as: xxxx EP04 xxxx.mp4 => 04 .
     */
    @Nonnull
    public static Double getEpFileNameIntegrallySeq(@Nonnull final String fileName) {
        AssertUtils.notBlank(fileName, "fileName");
        Set<String> strSet = new HashSet<>();

        Matcher matcher =
            Pattern.compile(RegexConst.FILE_NAME_EPISODE_SEQUENCE_WITH_INTEGRALLY_AND_BLANK)
                .matcher(fileName);
        while (matcher.find()) {
            strSet.add(matcher.group());
        }

        if (strSet.isEmpty()) {
            throw new RegexMatchingException(
                "file name episode seq matching exception , file name: "
                    + fileName);
        }

        return strSet.stream().findFirst()
            .map(String::trim)
            .map(str -> str.replace("EP", ""))
            .map(Double::parseDouble)
            .orElse(null);
    }

    /**
     * Get matching str by regex.
     */
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

    /**
     * Get matching english str.
     */
    @Nonnull
    public static String getMatchingEnglishStr(String str) {
        AssertUtils.notBlank(str, "str");
        final String regex = "[A-Za-z\\s]";
        return getMatchingStr(str, regex);
    }

    /**
     * Get matching english str without tag.
     */
    @Nonnull
    public static String getMatchingEnglishStrWithoutTag(String str) {
        AssertUtils.notBlank(str, "str");
        final String originalStr = str;
        str = str.replaceAll(RegexConst.FILE_NAME_TAG, "");
        str = str.replaceAll(RegexConst.FILE_POSTFIX, "");
        if (StringUtils.isBlank(str) || "[]".equalsIgnoreCase(str)) {
            log.warn("str is blank after remove file tag, originalStr={}", originalStr);
            // 针对全用中括号包裹的文件名称 获取第二个中括号的内容为标题
            List<String> fileTagList = getFileTag(originalStr);
            if (fileTagList.isEmpty() || fileTagList.size() <= 1) {
                return str;
            }
            return fileTagList.get(1);
        }
        final String regex = "[A-Za-z\\s]";
        return getMatchingStr(str, regex);
    }

    /**
     * Get matching chinese str.
     */
    @Nonnull
    public static String getMatchingChineseStr(String str) {
        AssertUtils.notBlank(str, "str");
        final String regex = "[\\u2E80-\\u9FFF]";
        return getMatchingStr(str, regex);
    }

    /**
     * Get matching chinese str without tag.
     */
    @Nonnull
    public static String getMatchingChineseStrWithoutTag(String str) {
        AssertUtils.notBlank(str, "str");
        final String originalStr = str;
        str = str.replaceAll(RegexConst.FILE_NAME_TAG, "");
        str = str.replaceAll(RegexConst.FILE_POSTFIX, "");
        str = str.replaceAll(RegexConst.FILE_NAME_TAG_EPISODE_SEQUENCE, "");
        if (StringUtils.isBlank(str)) {
            log.warn("str is blank after remove file tag, originalStr={}", originalStr);
            return str;
        }
        final String regex = "[\\u2E80-\\u9FFF]";
        return getMatchingStr(str, regex);
    }

    /**
     * Parse episode seq by file name.
     * <ul>
     *     <li>getFileNameBlankEpSeq: xxxx 04 xxxx. mp4 => 04 .</li>
     *     <li>getFileNameTagEpSeq: xxxxxxx[102.5] xxxxxx. mp4 => 102.5 , 支持中括号内0~999.0的浮点数.</li>
     *     <li>getEpFileNameIntegrallySeq: xxxx EP04 xxxx. mp4 => 04 .</li>
     *     <li>getFileNameHorizontalEpSeq: xxxx-04-xxxx. mp4 => 04 .</li>
     *     <li>getFileNameUnderlineEpSeq: xxxx_04_xxxx. mp4 => 04 .</li>
     * </ul>
     */
    @Nonnull
    public static Double parseEpisodeSeqByFileName(String fileName) {
        AssertUtils.notBlank(fileName, "fileName");
        Double seq = -1D;
        try {
            seq = getFileNameBlankEpSeq(fileName);
        } catch (RegexMatchingException e) {
            try {
                seq = getFileNameTagEpSeq(fileName);
            } catch (RegexMatchingException e1) {
                try {
                    seq = getEpFileNameIntegrallySeq(fileName);
                } catch (RegexMatchingException e3) {
                    try {
                        seq = getFileNameHorizontalEpSeq(fileName);
                    } catch (RegexMatchingException e4) {
                        try {
                            seq = getFileNameUnderlineEpSeq(fileName);
                        } catch (RegexMatchingException e5) {
                            log.warn("parse episode seq fail by file name: {}.", fileName, e5);
                        }
                    }
                }
            }
        }
        return seq;
    }

}
