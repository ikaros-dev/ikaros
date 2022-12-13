package run.ikaros.server.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author li-guohao
 */
public enum OptionCategory {
    APP,
    COMMON,
    SEO,
    FILE,
    NETWORK,
    /**
     * @see <a href="https://github.com/qbittorrent/qBittorrent">qBittorrent</a>
     */
    QBITTORRENT,
    /**
     * @see <a href="https://github.com/bangumi/api">bangumi/api</a>
     */
    BGMTV,
    /**
     * @see <a href="https://mikanani.me/">mikan</a>
     */
    MIKAN,
    /**
     * @see <a href="https://github.com/jellyfin/jellyfin">jellyfin</a>
     */
    JELLYFIN,
    NOTIFY,
    OTHER;

    public static final Set<String> CATEGORY_SET = Arrays.stream(OptionCategory.values())
        .flatMap(
            (Function<OptionCategory, Stream<String>>) optionCategory
                -> Stream.of(optionCategory.name())).collect(Collectors.toSet());
}
