package run.ikaros.api.core.subject;

import java.util.List;

/**
 * 剧集相关数据的组合，使用复杂的数据，会增加单个接口的耗时，但对整个条目来说，能有效降低并发请求次数.
 *
 * @param episode   剧集
 * @param resources 剧集附件资源集合
 */
public record EpisodeRecord(Episode episode, List<EpisodeResource> resources) {
}
