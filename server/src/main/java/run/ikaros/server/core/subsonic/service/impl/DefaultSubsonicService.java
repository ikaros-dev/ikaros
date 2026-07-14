package run.ikaros.server.core.subsonic.service.impl;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.AttachmentStreamVo;
import run.ikaros.api.core.music.Music;
import run.ikaros.api.core.music.Song;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.EpisodeResource;
import run.ikaros.api.core.subject.Subject;
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
import run.ikaros.api.store.enums.EpisodeGroup;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.service.AttachmentService;
import run.ikaros.server.core.episode.EpisodeService;
import run.ikaros.server.core.music.service.MusicService;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.core.subsonic.SubsonicContext;
import run.ikaros.server.core.subsonic.service.SubsonicService;
import run.ikaros.server.store.entity.EpisodeListEntity;
import run.ikaros.server.store.entity.EpisodeListEpisodeEntity;
import run.ikaros.server.store.entity.SubjectEntity;
import run.ikaros.server.store.repository.EpisodeListEpisodeRepository;
import run.ikaros.server.store.repository.EpisodeListRepository;
import run.ikaros.server.store.repository.SubjectRepository;

/**
 * Subsonic API 默认服务实现.
 * 对接 SubjectService / EpisodeService / AttachmentService / EpisodeList 等现有模块.
 *
 * @author Nekoli
 */
@Slf4j
@Service
public class DefaultSubsonicService implements SubsonicService {

    private static final String API_VERSION = "1.16.0";
    private static final String SERVER_TYPE = "ikaros";
    private static final String SERVER_VERSION = "1.2.1";

    private final MusicService musicService;
    private final SubjectService subjectService;
    private final SubjectRepository subjectRepository;
    private final EpisodeService episodeService;
    private final AttachmentService attachmentService;
    private final EpisodeListRepository episodeListRepository;
    private final EpisodeListEpisodeRepository episodeListEpisodeRepository;

    public DefaultSubsonicService(MusicService musicService,
                                  SubjectService subjectService,
                                  SubjectRepository subjectRepository,
                                  EpisodeService episodeService,
                                  AttachmentService attachmentService,
                                  EpisodeListRepository episodeListRepository,
                                  EpisodeListEpisodeRepository episodeListEpisodeRepository) {
        this.musicService = musicService;
        this.subjectService = subjectService;
        this.subjectRepository = subjectRepository;
        this.episodeService = episodeService;
        this.attachmentService = attachmentService;
        this.episodeListRepository = episodeListRepository;
        this.episodeListEpisodeRepository = episodeListEpisodeRepository;
    }

    // ========== 基础方法 ==========

    private SubsonicResponseBody ok() {
        return SubsonicResponseBody.builder()
            .status("ok").version(API_VERSION)
            .type(SERVER_TYPE).serverVersion(SERVER_VERSION)
            .build();
    }

    private SubsonicResponseBody err(int code, String message) {
        return SubsonicResponseBody.builder()
            .status("failed").version(API_VERSION)
            .type(SERVER_TYPE).serverVersion(SERVER_VERSION)
            .error(SubsonicResponse.Error.builder()
                .code(code).message(message).build())
            .build();
    }

    @Override
    public Mono<SubsonicContext> authenticate(String username, String password,
                                              String token, String salt) {
        // 简化认证：所有请求通过
        return Mono.just(SubsonicContext.builder()
            .username(username).authenticated(true).build());
    }

    @Override
    public Mono<SubsonicResponseBody> ping() {
        return Mono.just(ok());
    }

    // ========== 艺术家 ==========

    @Override
    public Mono<SubsonicResponseBody> getArtists() {
        return subjectRepository.findAllByType(SubjectType.MUSIC, null)
            .collectList()
            .map(subjects -> {
                List<ArtistIndex> indexes = new ArrayList<>();
                // 将音乐专辑作为 Artist 返回
                List<ArtistChild> artists = subjects.stream().map(sub -> {
                    // 取名称首字符作为索引分组
                    String name = sub.getNameCn() != null ? sub.getNameCn() : sub.getName();
                    return ArtistChild.builder()
                        .id(sub.getId().toString())
                        .name(name)
                        .albumCount(1)
                        .coverArt("al-" + sub.getId())
                        .build();
                }).toList();

                // 按首字母分组
                var groups = new java.util.LinkedHashMap<String, List<ArtistChild>>();
                for (ArtistChild a : artists) {
                    String key = a.getName().substring(0, 1).toUpperCase();
                    if (!key.matches("[A-Z]")) key = "#";
                    groups.computeIfAbsent(key, k -> new ArrayList<>()).add(a);
                }
                groups.forEach((letter, list) ->
                    indexes.add(ArtistIndex.builder().name(letter).artist(list).build()));

                return ok().toBuilder()
                    .artists(Artists.builder().index(indexes).build())
                    .build();
            });
    }

    @Override
    public Mono<SubsonicResponseBody> getArtist(String artistId) {
        UUID id = UuidV7Utils.fromString(artistId);
        return subjectService.findById(id)
            .flatMap(subject -> {
                String name = subject.getNameCn() != null ? subject.getNameCn() : subject.getName();
                // 获取该专辑下的歌曲数
                return episodeService.countBySubjectId(id)
                    .map(count -> ok().toBuilder()
                        .artist(SubsonicResponse.Artist.builder()
                            .id(id.toString())
                            .name(name)
                            .albumCount(1)
                            .coverArt("al-" + id)
                            .album(List.of(AlbumChild.builder()
                                .id(id.toString())
                                .name(name)
                                .coverArt("al-" + id)
                                .songCount((int) count)
                                .artist(name)
                                .parent(id.toString())
                                .build()))
                            .build())
                        .build());
            })
            .switchIfEmpty(Mono.just(err(70, "未找到艺术家: " + artistId)));
    }

    // ========== 专辑 ==========

    @Override
    public Mono<SubsonicResponseBody> getAlbum(String albumId) {
        UUID id = UuidV7Utils.fromString(albumId);
        Mono<Music> albumMono = musicService.findAlbumById(id);
        Mono<List<Song>> songsMono = musicService.listSongs(id).collectList();

        return Mono.zip(albumMono, songsMono)
            .flatMap(tuple -> {
                Music album = tuple.getT1();
                List<Song> songs = tuple.getT2();
                String name = album.getNameCn() != null ? album.getNameCn() : album.getName();
                String artist = album.getName();
                List<SongChild> songChildren = songs.stream().map(this::toSongChild).toList();
                long totalDuration = songs.stream()
                    .mapToLong(s -> s.getDuration() != null ? s.getDuration() : 0).sum();

                return Mono.just(ok().toBuilder()
                    .album(AlbumWithSongs.builder()
                        .id(album.getId().toString())
                        .name(name)
                        .artist(artist)
                        .coverArt("al-" + album.getId())
                        .songCount(songs.size())
                        .duration(formatDuration(totalDuration))
                        .created(album.getAirTime() != null
                            ? album.getAirTime().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            : null)
                        .parent(album.getId().toString())
                        .song(songChildren)
                        .build())
                    .build());
            })
            .switchIfEmpty(Mono.just(err(70, "未找到专辑: " + albumId)));
    }

    // ========== 歌曲 ==========

    @Override
    public Mono<SubsonicResponseBody> getSong(String songId) {
        UUID id = UuidV7Utils.fromString(songId);
        return episodeService.findById(id)
            .flatMap(episode -> {
                SongChild song = toSongChild(episode);
                if (episode.getSubjectId() != null) {
                    // 获取专辑名
                    return subjectService.findById(episode.getSubjectId())
                        .map(subject -> {
                            song.setAlbum(subject.getNameCn() != null ? subject.getNameCn() : subject.getName());
                            song.setArtist(subject.getName());
                            song.setAlbumId(subject.getId().toString());
                            return ok().toBuilder().song(song).build();
                        })
                        .switchIfEmpty(Mono.just(ok().toBuilder().song(song).build()));
                }
                return Mono.just(ok().toBuilder().song(song).build());
            })
            .switchIfEmpty(Mono.just(err(70, "未找到歌曲: " + songId)));
    }

    // ========== 专辑列表 ==========

    @Override
    public Mono<SubsonicResponseBody> getAlbumList2(String type, int size, int offset) {
        int page = (offset / Math.max(size, 1)) + 1;
        return musicService.listAlbums(page, size)
            .flatMap(wrap -> {
                List<AlbumChild> albums = wrap.getItems().stream()
                    .map(music -> AlbumChild.builder()
                        .id(music.getId().toString())
                        .name(music.getNameCn() != null ? music.getNameCn() : music.getName())
                        .artist(music.getName())
                        .coverArt("al-" + music.getId())
                        .songCount(music.getSongCount() != null ? music.getSongCount().intValue() : 0)
                        .created(music.getAirTime() != null
                            ? music.getAirTime().format(DateTimeFormatter.ISO_LOCAL_DATE)
                            : null)
                        .parent(music.getId().toString())
                        .build())
                    .toList();
                return Mono.just(ok().toBuilder()
                    .albumList(AlbumList.builder().album(albums).build())
                    .build());
            });
    }

    // ========== 搜索 ==========

    @Override
    public Mono<SubsonicResponseBody> search2(String query, int artistCount,
                                              int albumCount, int songCount) {
        return musicService.searchAlbums(query, 1, albumCount)
            .flatMap(wrap -> {
                List<AlbumChild> albums = wrap.getItems().stream()
                    .map(music -> AlbumChild.builder()
                        .id(music.getId().toString())
                        .name(music.getNameCn() != null ? music.getNameCn() : music.getName())
                        .artist(music.getName())
                        .coverArt("al-" + music.getId())
                        .songCount(music.getSongCount() != null ? music.getSongCount().intValue() : 0)
                        .parent(music.getId().toString())
                        .build())
                    .toList();
                return Mono.just(ok().toBuilder()
                    .searchResult(SearchResult.builder()
                        .artist(List.of())
                        .album(albums)
                        .song(List.of())
                        .build())
                    .build());
            });
    }

    // ========== 音频流 ==========

    @Override
    public Mono<Resource> stream(String songId) {
        UUID id = UuidV7Utils.fromString(songId);
        return episodeService.findResourcesById(id)
            .next()
            .flatMap(res -> attachmentService.getStreamById(res.getAttachmentId()))
            .flatMap(streamVo -> streamVo.getDataBufferFlux()
                .collectList()
                .map(bufs -> {
                    var os = new java.io.ByteArrayOutputStream();
                    bufs.forEach(buf -> {
                        byte[] b = new byte[buf.readableByteCount()];
                        buf.read(b);
                        try { os.write(b); } catch (Exception ignored) {}
                    });
                    return (Resource) new org.springframework.core.io.ByteArrayResource(os.toByteArray());
                }));
    }

    // ========== 封面 ==========

    @Override
    public Mono<Resource> getCoverArt(String albumId, int size) {
        // albumId 格式: "al-{uuid}" 或直接 uuid
        String rawId = albumId.startsWith("al-") ? albumId.substring(3) : albumId;
        UUID id = UuidV7Utils.fromString(rawId);
        return subjectService.findById(id)
            .flatMap(subject -> {
                if (!StringUtils.hasText(subject.getCover())) {
                    return Mono.error(new RuntimeException("该专辑无封面"));
                }
                try {
                    URI uri = URI.create(subject.getCover());
                    return Mono.just((Resource) new UrlResource(uri));
                } catch (Exception e) {
                    return Mono.error(new RuntimeException("无法读取封面: " + e.getMessage()));
                }
            });
    }

    // ========== 歌单 ==========

    @Override
    public Mono<SubsonicResponseBody> getPlaylists() {
        return episodeListRepository.findAll()
            .collectList()
            .flatMap(lists -> {
                List<PlaylistChild> playlists = lists.stream()
                    .map(epList -> PlaylistChild.builder()
                        .id(epList.getId().toString())
                        .name(epList.getName())
                        .comment(epList.getDescription())
                        .songCount(0)
                        .owner("admin")
                        .isPublic(!Boolean.TRUE.equals(epList.getNsfw()))
                        .build())
                    .toList();
                return Mono.just(ok().toBuilder()
                    .playlists(SubsonicResponse.Playlists.builder()
                        .playlist(playlists).build())
                    .build());
            });
    }

    @Override
    public Mono<SubsonicResponseBody> getPlaylist(String playlistId) {
        UUID id = UuidV7Utils.fromString(playlistId);
        return episodeListRepository.findById(id)
            .flatMap(epList ->
                episodeListEpisodeRepository.findAllByEpisodeListId(id)
                    .collectList()
                    .flatMap(episodes -> {
                        // 收集所有歌曲信息
                        List<Mono<SongChild>> songMonos = episodes.stream()
                            .map(ep -> episodeService.findById(ep.getEpisodeId())
                                .map(this::toSongChild)
                                .onErrorResume(e -> Mono.empty()))
                            .toList();
                        return Mono.when(songMonos)
                            .then(Mono.defer(() -> {
                                List<SongChild> songs = songMonos.stream()
                                    .map(Mono::block)
                                    .filter(java.util.Objects::nonNull)
                                    .toList();
                                long duration = songs.stream()
                                    .mapToLong(SongChild::getDuration).sum();
                                return Mono.just(ok().toBuilder()
                                    .playlist(PlaylistWithSongs.builder()
                                        .id(epList.getId().toString())
                                        .name(epList.getName())
                                        .comment(epList.getDescription())
                                        .songCount(songs.size())
                                        .duration(formatDuration(duration))
                                        .owner("admin")
                                        .isPublic(!Boolean.TRUE.equals(epList.getNsfw()))
                                        .entry(songs)
                                        .build())
                                    .build());
                            }));
                    }))
            .switchIfEmpty(Mono.just(err(70, "未找到歌单: " + playlistId)));
    }

    @Override
    public Mono<SubsonicResponseBody> createPlaylist(String playlistId, String name,
                                                     List<String> songIds) {
        if (StringUtils.hasText(playlistId)) {
            // 更新已有歌单
            UUID id = UuidV7Utils.fromString(playlistId);
            return episodeListRepository.findById(id)
                .flatMap(existing -> {
                    existing.setName(name);
                    return episodeListRepository.save(existing)
                        .flatMap(saved -> updatePlaylistSongs(id, songIds));
                })
                .then(Mono.just(ok()));
        }
        // 新建歌单
        EpisodeListEntity newList = EpisodeListEntity.builder()
            .name(name)
            .description("")
            .nsfw(false)
            .build();
        newList.setId(UuidV7Utils.generateUuid());
        return episodeListRepository.save(newList)
            .flatMap(saved -> updatePlaylistSongs(saved.getId(), songIds))
            .then(Mono.just(ok()));
    }

    @Override
    public Mono<SubsonicResponseBody> deletePlaylist(String playlistId) {
        UUID id = UuidV7Utils.fromString(playlistId);
        return episodeListRepository.deleteById(id)
            .then(Mono.just(ok()));
    }

    // ========== 播放记录 ==========

    @Override
    public Mono<SubsonicResponseBody> scrobble(String songId, long time, boolean submission) {
        // 播放记录后续通过 EpisodeCollection 持久化
        log.debug("Scrobble: songId={}, time={}, submission={}", songId, time, submission);
        return Mono.just(ok());
    }

    // ========== 工具方法 ==========

    private SongChild toSongChild(Episode episode) {
        return SongChild.builder()
            .id(episode.getId().toString())
            .parent(episode.getSubjectId() != null ? episode.getSubjectId().toString() : "")
            .title(episode.getNameCn() != null ? episode.getNameCn() : episode.getName())
            .album("")
            .artist("")
            .track(episode.getSequence() != null ? episode.getSequence().intValue() : 0)
            .duration(0)
            .contentType("audio/mpeg")
            .coverArt(episode.getSubjectId() != null ? "al-" + episode.getSubjectId() : "")
            .isDir(false)
            .year(0)
            .albumId(episode.getSubjectId() != null ? episode.getSubjectId().toString() : "")
            .type("music")
            .build();
    }

    private SongChild toSongChildWithAttachment(Episode episode) {
        SongChild song = toSongChild(episode);
        // 尝试获取附件信息
        try {
            EpisodeResource res = episodeService.findResourcesById(episode.getId())
                .next().block();
            if (res != null) {
                song.setId(res.getAttachmentId().toString());
                song.setContentType("audio/mpeg");
                song.setSize(0);
                song.setSuffix("mp3");
                song.setPath(episode.getName());
            }
        } catch (Exception e) {
            // 忽略附件查询异常
        }
        return song;
    }

    private Mono<Void> updatePlaylistSongs(UUID listId, List<String> songIds) {
        if (songIds == null || songIds.isEmpty()) {
            return Mono.empty();
        }
        return episodeListEpisodeRepository.deleteByEpisodeListId(listId)
            .thenMany(episodeListEpisodeRepository.saveAll(songIds.stream()
                .map(sid -> {
                    EpisodeListEpisodeEntity e = new EpisodeListEpisodeEntity();
                    e.setId(UuidV7Utils.generateUuid());
                    e.setEpisodeListId(listId);
                    e.setEpisodeId(UuidV7Utils.fromString(sid));
                    return e;
                }).toList()))
            .then();
    }

    private String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }
}
