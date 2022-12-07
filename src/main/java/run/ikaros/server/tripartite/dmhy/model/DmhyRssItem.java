package run.ikaros.server.tripartite.dmhy.model;

import lombok.Data;
import run.ikaros.server.tripartite.dmhy.enums.DmhyCategory;

@Data
public class DmhyRssItem {
    private String title;
    private String link;
    private String pubDate;
    private String description;
    private String magnetUrl;
    private String author;
    private DmhyCategory category;
}
