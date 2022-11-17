package run.ikaros.server.tripartite.mikan.model;

/**
 * mikan project my subscribe rss item java model
 *
 * @author li-guohao
 * @link <a href="https://mikanani.me/">Mikan Project</a>
 */
public class MikanRssItem {
    private String title;
    private String torrentUrl;
    private String episodePageUrl;

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

    public String getEpisodePageUrl() {
        return episodePageUrl;
    }

    public MikanRssItem setEpisodePageUrl(String episodePageUrl) {
        this.episodePageUrl = episodePageUrl;
        return this;
    }

}
