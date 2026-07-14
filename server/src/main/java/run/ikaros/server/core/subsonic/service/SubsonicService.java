package run.ikaros.server.core.subsonic.service;

import java.util.Optional;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subsonic.SubsonicResponse.SubsonicResponseBody;
import run.ikaros.server.core.subsonic.SubsonicContext;

/**
 * Subsonic API 服务接口.
 * 实现 Subsonic 协议的核心方法，提供音乐流媒体浏览、播放、搜索和歌单管理功能.
 *
 * @author Nekoli
 */
public interface SubsonicService {

    /**
     * 认证用户.
     */
    Mono<SubsonicContext> authenticate(String username, String password,
                                       String token, String salt);

    /**
     * ping — 检查服务状态.
     */
    Mono<SubsonicResponseBody> ping();

    /**
     * getArtists — 获取所有艺术家列表.
     */
    Mono<SubsonicResponseBody> getArtists();

    /**
     * getArtist — 获取艺术家详情（含专辑）.
     */
    Mono<SubsonicResponseBody> getArtist(String artistId);

    /**
     * getAlbum — 获取专辑详情（含歌曲）.
     */
    Mono<SubsonicResponseBody> getAlbum(String albumId);

    /**
     * getSong — 获取歌曲详情.
     */
    Mono<SubsonicResponseBody> getSong(String songId);

    /**
     * getAlbumList2 — 获取专辑列表（按类型）.
     */
    Mono<SubsonicResponseBody> getAlbumList2(String type, int size, int offset);

    /**
     * search2 — 搜索.
     */
    Mono<SubsonicResponseBody> search2(String query, int artistCount,
                                       int albumCount, int songCount);

    /**
     * stream — 获取音频流.
     */
    Mono<org.springframework.core.io.Resource> stream(String songId);

    /**
     * getCoverArt — 获取封面图.
     */
    Mono<org.springframework.core.io.Resource> getCoverArt(String albumId, int size);

    /**
     * getPlaylists — 获取歌单列表.
     */
    Mono<SubsonicResponseBody> getPlaylists();

    /**
     * getPlaylist — 获取歌单详情（含歌曲）.
     */
    Mono<SubsonicResponseBody> getPlaylist(String playlistId);

    /**
     * createPlaylist — 创建或更新歌单.
     */
    Mono<SubsonicResponseBody> createPlaylist(String playlistId, String name,
                                               java.util.List<String> songIds);

    /**
     * deletePlaylist — 删除歌单.
     */
    Mono<SubsonicResponseBody> deletePlaylist(String playlistId);

    /**
     * scrobble — 记录播放.
     */
    Mono<SubsonicResponseBody> scrobble(String songId, long time, boolean submission);
}
