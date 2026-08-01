package run.ikaros.server.core.music.service;

import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.music.Music;
import run.ikaros.api.core.music.Song;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.wrap.PagingWrap;

/**
 * 音乐模块服务接口.
 * 提供音乐专辑（SubjectType.MUSIC）和歌曲（Episode）的增删改查功能.
 *
 * @author Nekoli
 */
public interface MusicService {

    /**
     * 分页查询音乐专辑列表.
     *
     * @param page 页码，从1开始
     * @param size 每页条数
     * @return 分页的音乐专辑列表
     */
    Mono<PagingWrap<Music>> listAlbums(int page, int size);

    /**
     * 根据ID查询单个音乐专辑.
     *
     * @param id 专辑ID
     * @return 音乐专辑信息
     */
    Mono<Music> findAlbumById(UUID id);

    /**
     * 创建音乐专辑.
     *
     * @param subject 专辑信息
     * @return 创建后的专辑
     */
    Mono<Subject> createAlbum(Subject subject);

    /**
     * 更新音乐专辑.
     *
     * @param subject 专辑信息
     */
    Mono<Void> updateAlbum(Subject subject);

    /**
     * 删除音乐专辑.
     *
     * @param id 专辑ID
     */
    Mono<Void> deleteAlbum(UUID id);

    /**
     * 查询专辑下的所有歌曲.
     *
     * @param subjectId 专辑ID
     * @return 歌曲列表
     */
    Flux<Song> listSongs(UUID subjectId);

    /**
     * 添加歌曲到专辑.
     *
     * @param episode 歌曲信息
     * @return 添加后的歌曲
     */
    Mono<Episode> addSong(Episode episode);

    /**
     * 更新歌曲信息.
     *
     * @param episode 歌曲信息
     */
    Mono<Void> updateSong(Episode episode);

    /**
     * 删除歌曲.
     *
     * @param id 歌曲ID
     */
    Mono<Void> deleteSong(UUID id);

    /**
     * 搜索音乐专辑.
     *
     * @param keyword 关键词
     * @param page    页码
     * @param size    每页条数
     * @return 分页搜索的结果
     */
    Mono<PagingWrap<Music>> searchAlbums(String keyword, int page, int size);
}
