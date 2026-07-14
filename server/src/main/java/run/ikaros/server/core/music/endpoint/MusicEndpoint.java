package run.ikaros.server.core.music.endpoint;

import static org.springdoc.core.fn.builders.apiresponse.Builder.responseBuilder;
import static org.springdoc.core.fn.builders.parameter.Builder.parameterBuilder;
import static org.springdoc.core.fn.builders.requestbody.Builder.requestBodyBuilder;

import io.swagger.v3.oas.annotations.enums.ParameterIn;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.webflux.core.fn.SpringdocRouteBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.constant.OpenApiConst;
import run.ikaros.api.core.music.Music;
import run.ikaros.api.core.music.Song;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.infra.utils.UuidV7Utils;
import run.ikaros.server.core.music.service.MusicService;
import run.ikaros.server.endpoint.CoreEndpoint;

/**
 * 音乐模块端点.
 * 提供音乐专辑和歌曲的 REST API，基于已有数据模型 SubjectType.MUSIC 和 Episode 实现.
 *
 * @author Nekoli
 */
@Slf4j
@Component
public class MusicEndpoint implements CoreEndpoint {

    private final MusicService musicService;

    public MusicEndpoint(MusicService musicService) {
        this.musicService = musicService;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        var tag = OpenApiConst.CORE_VERSION + "/music";
        return SpringdocRouteBuilder.route()
            .GET("/music/albums/{page}/{size}", this::listAlbums,
                builder -> builder.operationId("ListMusicAlbums")
                    .tag(tag)
                    .description("分页查询音乐专辑列表")
                    .parameter(parameterBuilder()
                        .name("page").in(ParameterIn.PATH)
                        .required(true).implementation(Integer.class)
                        .description("页码，从1开始"))
                    .parameter(parameterBuilder()
                        .name("size").in(ParameterIn.PATH)
                        .required(true).implementation(Integer.class)
                        .description("每页条数"))
                    .response(responseBuilder()
                        .description("分页的音乐专辑列表")
                        .implementationArray(Music.class))
            )
            .GET("/music/album/{id}", this::getAlbum,
                builder -> builder.operationId("GetMusicAlbum")
                    .tag(tag).description("根据ID查询单个音乐专辑")
                    .parameter(parameterBuilder()
                        .name("id").in(ParameterIn.PATH)
                        .required(true).implementation(String.class)
                        .description("专辑ID"))
                    .response(responseBuilder()
                        .description("音乐专辑信息")
                        .implementation(Music.class))
            )
            .POST("/music/album", this::createAlbum,
                builder -> builder.operationId("CreateMusicAlbum")
                    .tag(tag).description("创建新的音乐专辑")
                    .requestBody(requestBodyBuilder()
                        .description("专辑信息，type 会自动设置为 MUSIC")
                        .implementation(Subject.class))
                    .response(responseBuilder()
                        .description("创建后的专辑")
                        .implementation(Subject.class))
            )
            .PUT("/music/album", this::updateAlbum,
                builder -> builder.operationId("UpdateMusicAlbum")
                    .tag(tag).description("更新音乐专辑信息")
                    .requestBody(requestBodyBuilder()
                        .description("专辑信息")
                        .implementation(Subject.class))
            )
            .DELETE("/music/album/{id}", this::deleteAlbum,
                builder -> builder.operationId("DeleteMusicAlbum")
                    .tag(tag).description("删除音乐专辑")
                    .parameter(parameterBuilder()
                        .name("id").in(ParameterIn.PATH)
                        .required(true).implementation(String.class)
                        .description("专辑ID"))
            )
            .GET("/music/album/{id}/songs", this::listSongs,
                builder -> builder.operationId("ListMusicSongs")
                    .tag(tag).description("查询专辑下的所有歌曲")
                    .parameter(parameterBuilder()
                        .name("id").in(ParameterIn.PATH)
                        .required(true).implementation(String.class)
                        .description("专辑ID"))
                    .response(responseBuilder()
                        .description("歌曲列表")
                        .implementationArray(Song.class))
            )
            .POST("/music/song", this::addSong,
                builder -> builder.operationId("AddMusicSong")
                    .tag(tag).description("添加歌曲到专辑")
                    .requestBody(requestBodyBuilder()
                        .description("歌曲（Episode）信息，需包含 subjectId")
                        .implementation(Episode.class))
                    .response(responseBuilder()
                        .description("添加后的歌曲")
                        .implementation(Episode.class))
            )
            .PUT("/music/song", this::updateSong,
                builder -> builder.operationId("UpdateMusicSong")
                    .tag(tag).description("更新歌曲信息")
                    .requestBody(requestBodyBuilder()
                        .description("歌曲信息")
                        .implementation(Episode.class))
            )
            .DELETE("/music/song/{id}", this::deleteSong,
                builder -> builder.operationId("DeleteMusicSong")
                    .tag(tag).description("删除歌曲")
                    .parameter(parameterBuilder()
                        .name("id").in(ParameterIn.PATH)
                        .required(true).implementation(String.class)
                        .description("歌曲ID"))
            )
            .GET("/music/search/{keyword}/{page}/{size}", this::searchAlbums,
                builder -> builder.operationId("SearchMusicAlbums")
                    .tag(tag).description("搜索音乐专辑")
                    .parameter(parameterBuilder()
                        .name("keyword").in(ParameterIn.PATH)
                        .required(true).implementation(String.class)
                        .description("搜索关键词"))
                    .parameter(parameterBuilder()
                        .name("page").in(ParameterIn.PATH)
                        .required(true).implementation(Integer.class)
                        .description("页码"))
                    .parameter(parameterBuilder()
                        .name("size").in(ParameterIn.PATH)
                        .required(true).implementation(Integer.class)
                        .description("每页条数"))
                    .response(responseBuilder()
                        .description("分页搜索结果")
                        .implementationArray(Music.class))
            )
            .build();
    }

    private Mono<ServerResponse> listAlbums(ServerRequest request) {
        int page = Integer.parseInt(request.pathVariable("page"));
        int size = Integer.parseInt(request.pathVariable("size"));
        return musicService.listAlbums(page, size)
            .flatMap(wrap -> ServerResponse.ok().bodyValue(wrap));
    }

    private Mono<ServerResponse> getAlbum(ServerRequest request) {
        UUID id = UuidV7Utils.fromString(request.pathVariable("id"));
        return musicService.findAlbumById(id)
            .flatMap(music -> ServerResponse.ok().bodyValue(music));
    }

    private Mono<ServerResponse> createAlbum(ServerRequest request) {
        return request.bodyToMono(Subject.class)
            .flatMap(musicService::createAlbum)
            .flatMap(subject -> ServerResponse.ok().bodyValue(subject));
    }

    private Mono<ServerResponse> updateAlbum(ServerRequest request) {
        return request.bodyToMono(Subject.class)
            .flatMap(musicService::updateAlbum)
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> deleteAlbum(ServerRequest request) {
        UUID id = UuidV7Utils.fromString(request.pathVariable("id"));
        return musicService.deleteAlbum(id)
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> listSongs(ServerRequest request) {
        UUID subjectId = UuidV7Utils.fromString(request.pathVariable("id"));
        return musicService.listSongs(subjectId)
            .collectList()
            .flatMap(songs -> ServerResponse.ok().bodyValue(songs));
    }

    private Mono<ServerResponse> addSong(ServerRequest request) {
        return request.bodyToMono(Episode.class)
            .flatMap(musicService::addSong)
            .flatMap(episode -> ServerResponse.ok().bodyValue(episode));
    }

    private Mono<ServerResponse> updateSong(ServerRequest request) {
        return request.bodyToMono(Episode.class)
            .flatMap(musicService::updateSong)
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> deleteSong(ServerRequest request) {
        UUID id = UuidV7Utils.fromString(request.pathVariable("id"));
        return musicService.deleteSong(id)
            .then(ServerResponse.ok().build());
    }

    private Mono<ServerResponse> searchAlbums(ServerRequest request) {
        String keyword = request.pathVariable("keyword");
        int page = Integer.parseInt(request.pathVariable("page"));
        int size = Integer.parseInt(request.pathVariable("size"));
        return musicService.searchAlbums(keyword, page, size)
            .flatMap(wrap -> ServerResponse.ok().bodyValue(wrap));
    }
}
