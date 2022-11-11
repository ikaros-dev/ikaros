package run.ikaros.server.rss.mikan.model;

/**
 * mikan project my subscribe rss item java model
 *
 * @author li-guohao
 * @link <a href="https://mikanani.me/">Mikan Project</a>
 */
public class MikanRssItem {
    private String title;
    private String torrentUrl;

    public String getTitle() {
        return title;
    }

    public MikanRssItem setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getTorrentUrl() {
        return torrentUrl;
    }

    public MikanRssItem setTorrentUrl(String torrentUrl) {
        this.torrentUrl = torrentUrl;
        return this;
    }
}
