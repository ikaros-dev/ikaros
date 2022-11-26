package run.ikaros.server.core.service;

/**
 * 媒体目录相关，如生成Jellyfin的nfo媒体目录
 */
public interface MediaService {
    void generateMediaDir();
}
