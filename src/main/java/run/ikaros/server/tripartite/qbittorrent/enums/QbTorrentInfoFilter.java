package run.ikaros.server.tripartite.qbittorrent.enums;

/**
 * @author li-guohao
 */
public enum QbTorrentInfoFilter {
    ALL("all"),
    DOWNLOADING("downloading"),
    SEEDING("seeding"),
    COMPLETED("completed"),
    PAUSED("paused"),
    ACTIVE("active"),
    INACTIVE("inactive"),
    RESUMED("resumed"),
    STALLED("stalled"),
    STALLED_UPLOADING("stalled_uploading"),
    STALLED_DOWNLOADING("stalled_downloading"),
    ERRORED("errored"),
    ;

    private final String value;

    QbTorrentInfoFilter(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
