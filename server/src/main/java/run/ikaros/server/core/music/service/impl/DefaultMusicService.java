package run.ikaros.server.core.music.service.impl;

import java.util.Base64;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.music.Music;
import run.ikaros.api.core.music.Song;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.vo.FindSubjectCondition;
import run.ikaros.api.infra.exception.NotFoundException;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.episode.EpisodeService;
import run.ikaros.server.core.music.service.MusicService;
import run.ikaros.server.core.subject.service.SubjectService;

/**
 * 音乐模块默认服务实现.
 * 提供音乐专辑（SubjectType.MUSIC）和歌曲（Episode）的增删改查功能.
 *
 * @author Nekoli
 */
@Slf4j
@Service
public class DefaultMusicService implements MusicService {

    private final SubjectService subjectService;

    private final EpisodeService episodeService;

    public DefaultMusicService(SubjectService subjectService,
                               EpisodeService episodeService) {
        this.subjectService = subjectService;
        this.episodeService = episodeService;
    }

    @Override
    public Mono<PagingWrap<Music>> listAlbums(int page, int size) {
        FindSubjectCondition condition = FindSubjectCondition.builder()
            .page(page).size(size)
            .type(SubjectType.MUSIC)
            .airTimeDesc(true)
            .build();
        condition.initDefaultIfNull();
        return subjectService.listEntitiesByCondition(condition)
            .flatMap(wrap -> {
                var musicList = wrap.getItems().stream()
                    .map(this::toMusic)
                    .toList();
                return Mono.just(new PagingWrap<>(page, size, wrap.getTotal(), musicList));
            });
    }

    @Override
    public Mono<Music> findAlbumById(UUID id) {
        return subjectService.findById(id)
            .filter(subject -> SubjectType.MUSIC.equals(subject.getType()))
            .flatMap(subject ->
                episodeService.countBySubjectId(subject.getId())
                    .map(count -> {
                        Music music = toMusic(subject);
                        music.setSongCount(count);
                        return music;
                    })
            )
            .switchIfEmpty(Mono.error(
                new NotFoundException("未找到音乐专辑，id: " + id)));
    }

    @Override
    public Mono<Subject> createAlbum(Subject subject) {
        subject.setType(SubjectType.MUSIC);
        return subjectService.create(subject);
    }

    @Override
    public Mono<Void> updateAlbum(Subject subject) {
        return subjectService.findById(subject.getId())
            .flatMap(existing -> {
                subject.setType(SubjectType.MUSIC);
                return subjectService.update(subject);
            });
    }

    @Override
    public Mono<Void> deleteAlbum(UUID id) {
        return subjectService.deleteById(id);
    }

    @Override
    public Flux<Song> listSongs(UUID subjectId) {
        return episodeService.findAllBySubjectId(subjectId)
            .map(this::toSong);
    }

    @Override
    public Mono<Episode> addSong(Episode episode) {
        return episodeService.save(episode);
    }

    @Override
    public Mono<Void> updateSong(Episode episode) {
        return episodeService.save(episode).then();
    }

    @Override
    public Mono<Void> deleteSong(UUID id) {
        return episodeService.deleteById(id);
    }

    @Override
    public Mono<PagingWrap<Music>> searchAlbums(String keyword, int page, int size) {
        FindSubjectCondition condition = FindSubjectCondition.builder()
            .page(page).size(size)
            .type(SubjectType.MUSIC)
            .name(Base64.getEncoder().encodeToString(keyword.getBytes()))
            .airTimeDesc(true)
            .build();
        condition.initDefaultIfNull();
        return subjectService.listEntitiesByCondition(condition)
            .flatMap(wrap -> {
                var musicList = wrap.getItems().stream()
                    .map(this::toMusic)
                    .toList();
                return Mono.just(new PagingWrap<>(page, size, wrap.getTotal(), musicList));
            });
    }

    /**
     * 将 Subject 转换为 Music DTO.
     */
    private Music toMusic(Subject subject) {
        return Music.builder()
            .id(subject.getId())
            .name(subject.getName())
            .nameCn(subject.getNameCn())
            .cover(subject.getCover())
            .description(subject.getSummary())
            .airTime(subject.getAirTime())
            .score(subject.getScore() == null ? null : subject.getScore().floatValue())
            .nsfw(subject.getNsfw())
            .songCount(0L)
            .build();
    }

    /**
     * 将 Episode 转换为 Song DTO.
     */
    private Song toSong(Episode episode) {
        return Song.builder()
            .id(episode.getId())
            .subjectId(episode.getSubjectId())
            .name(episode.getName())
            .nameCn(episode.getNameCn())
            .description(episode.getDescription())
            .airTime(episode.getAirTime())
            .sequence(episode.getSequence())
            .group(episode.getGroup() != null ? episode.getGroup().name() : null)
            .build();
    }
}
