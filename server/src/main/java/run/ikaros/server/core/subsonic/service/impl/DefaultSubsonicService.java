package run.ikaros.server.core.subsonic.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subsonic.SubsonicResponse;
import run.ikaros.api.core.subsonic.SubsonicResponse.AlbumChild;
import run.ikaros.api.core.subsonic.SubsonicResponse.AlbumList;
import run.ikaros.api.core.subsonic.SubsonicResponse.AlbumWithSongs;
import run.ikaros.api.core.subsonic.SubsonicResponse.ArtistChild;
import run.ikaros.api.core.subsonic.SubsonicResponse.ArtistIndex;
import run.ikaros.api.core.subsonic.SubsonicResponse.Artists;
import run.ikaros.api.core.subsonic.SubsonicResponse.PlaylistChild;
import run.ikaros.api.core.subsonic.SubsonicResponse.PlaylistWithSongs;
import run.ikaros.api.core.subsonic.SubsonicResponse.SearchResult;
import run.ikaros.api.core.subsonic.SubsonicResponse.SongChild;
import run.ikaros.api.core.subsonic.SubsonicResponse.SubsonicResponseBody;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.core.subsonic.SubsonicContext;
import run.ikaros.server.core.subsonic.service.SubsonicService;

/**
 * Subsonic API 默认服务实现.
 *
 * @author Nekoli
 */
@Slf4j
@Service
public class DefaultSubsonicService implements SubsonicService {

    private static final String API_VERSION = "1.16.0";
    private static final String SERVER_TYPE = "ikaros";
    private static final String SERVER_VERSION = "1.2.1";

    public DefaultSubsonicService() {
    }

    @Override
    public Mono<SubsonicContext> authenticate(String username, String password,
                                              String token, String salt) {
        // 简易认证：所有请求通过即可
        return Mono.just(SubsonicContext.builder()
            .username(username)
            .authenticated(true)
            .build());
    }

    private SubsonicResponseBody ok() {
        return SubsonicResponseBody.builder()
            .status("ok")
            .version(API_VERSION)
            .type(SERVER_TYPE)
            .serverVersion(SERVER_VERSION)
            .build();
    }

    private SubsonicResponseBody err(int code, String message) {
        return SubsonicResponseBody.builder()
            .status("failed")
            .version(API_VERSION)
            .type(SERVER_TYPE)
            .serverVersion(SERVER_VERSION)
            .error(SubsonicResponse.Error.builder()
                .code(code).message(message).build())
            .build();
    }

    @Override
    public Mono<SubsonicResponseBody> ping() {
        return Mono.just(ok());
    }

    @Override
    public Mono<SubsonicResponseBody> getArtists() {
        // 占位：返回空列表，待对接 SubjectService
        return Mono.just(ok().toBuilder()
            .artists(Artists.builder()
                .index(List.of())
                .build())
            .build());
    }

    @Override
    public Mono<SubsonicResponseBody> getArtist(String artistId) {
        return Mono.just(err(70, "艺术家功能暂未实现"));
    }

    @Override
    public Mono<SubsonicResponseBody> getAlbum(String albumId) {
        return Mono.just(err(70, "专辑详情功能暂未实现"));
    }

    @Override
    public Mono<SubsonicResponseBody> getSong(String songId) {
        return Mono.just(err(70, "歌曲详情功能暂未实现"));
    }

    @Override
    public Mono<SubsonicResponseBody> getAlbumList2(String type, int size, int offset) {
        return Mono.just(err(70, "专辑列表功能暂未实现"));
    }

    @Override
    public Mono<SubsonicResponseBody> search2(String query, int artistCount,
                                              int albumCount, int songCount) {
        return Mono.just(err(70, "搜索功能暂未实现"));
    }

    @Override
    public Mono<Resource> stream(String songId) {
        return Mono.error(new UnsupportedOperationException("音频流功能暂未实现"));
    }

    @Override
    public Mono<Resource> getCoverArt(String albumId, int size) {
        return Mono.error(new UnsupportedOperationException("封面图功能暂未实现"));
    }

    @Override
    public Mono<SubsonicResponseBody> getPlaylists() {
        return Mono.just(ok().toBuilder()
            .playlists(SubsonicResponse.Playlists.builder()
                .playlist(List.of())
                .build())
            .build());
    }

    @Override
    public Mono<SubsonicResponseBody> getPlaylist(String playlistId) {
        return Mono.just(err(70, "歌单详情功能暂未实现"));
    }

    @Override
    public Mono<SubsonicResponseBody> createPlaylist(String playlistId, String name,
                                                     List<String> songIds) {
        return Mono.just(err(70, "歌单创建功能暂未实现"));
    }

    @Override
    public Mono<SubsonicResponseBody> deletePlaylist(String playlistId) {
        return Mono.just(err(70, "歌单删除功能暂未实现"));
    }

    @Override
    public Mono<SubsonicResponseBody> scrobble(String songId, long time, boolean submission) {
        return Mono.just(ok());
    }
}
