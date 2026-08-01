package run.ikaros.server.core.subsonic.endpoint;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.subsonic.SubsonicResponse;
import run.ikaros.api.core.subsonic.SubsonicResponse.SubsonicResponseBody;
import run.ikaros.server.core.subsonic.SubsonicContext;
import run.ikaros.server.core.subsonic.service.SubsonicService;

/**
 * Subsonic API 路由.
 * 实现 Subsonic REST API 协议的核心端点.
 *
 * @author Nekoli
 */
@Slf4j
@Component
public class SubsonicRouter implements RouterFunction<ServerResponse> {

    private final SubsonicService subsonicService;

    public SubsonicRouter(SubsonicService subsonicService) {
        this.subsonicService = subsonicService;
    }

    @Override
    public Mono<HandlerFunction<ServerResponse>> route(ServerRequest request) {
        String path = request.path();
        if (!path.startsWith("/rest/")) {
            return Mono.empty();
        }
        return Mono.just(handler(request));
    }

    private HandlerFunction<ServerResponse> handler(ServerRequest request) {
        return req -> handleRequest(request);
    }

    @Override
    public void accept(RouterFunctions.Visitor visitor) {
        visitor.unknown(this);
    }

    private Mono<ServerResponse> handleRequest(ServerRequest request) {
        String path = request.path();
        String method = extractMethod(path);

        // 提取认证参数
        String u = request.queryParam("u").orElse("");
        String p = request.queryParam("p").orElse("");
        String t = request.queryParam("t").orElse("");
        String s = request.queryParam("s").orElse("");
        String f = request.queryParam("f").orElse("json");

        // 认证
        return subsonicService.authenticate(u, p, t, s)
            .flatMap(ctx -> processRequest(ctx, method, request))
            .onErrorResume(e -> {
                log.error("Subsonic API error: {}", e.getMessage());
                SubsonicResponseBody errBody = SubsonicResponseBody.builder()
                    .status("failed")
                    .version("1.16.0")
                    .type("ikaros")
                    .serverVersion("1.2.1")
                    .error(SubsonicResponse.Error.builder()
                        .code(0).message(e.getMessage()).build())
                    .build();
                return formatResponse(errBody, f);
            });
    }

    private Mono<ServerResponse> processRequest(SubsonicContext ctx,
                                                  String method,
                                                  ServerRequest request) {
        // stream 和 getCoverArt 返回二进制流，单独处理
        if ("stream".equals(method)) {
            String id = request.queryParam("id").orElse("");
            return handleStream(id);
        }
        if ("getCoverArt".equals(method)) {
            String id = request.queryParam("id").orElse("");
            int size = Integer.parseInt(request.queryParam("size").orElse("0"));
            return handleCoverArt(id, size);
        }

        // 其余方法返回 JSON
        Mono<SubsonicResponseBody> bodyMono = switch (method) {
            case "ping" -> subsonicService.ping();
            case "getArtists" -> subsonicService.getArtists();
            case "getArtist" -> {
                String id = request.queryParam("id").orElse("");
                yield subsonicService.getArtist(id);
            }
            case "getAlbum" -> {
                String id = request.queryParam("id").orElse("");
                yield subsonicService.getAlbum(id);
            }
            case "getSong" -> {
                String id = request.queryParam("id").orElse("");
                yield subsonicService.getSong(id);
            }
            case "getAlbumList2" -> {
                String type = request.queryParam("type").orElse("random");
                int size = Integer.parseInt(request.queryParam("size").orElse("10"));
                int offset = Integer.parseInt(request.queryParam("offset").orElse("0"));
                yield subsonicService.getAlbumList2(type, size, offset);
            }
            case "search2" -> {
                String query = request.queryParam("query").orElse("");
                int artistCount = Integer.parseInt(
                    request.queryParam("artistCount").orElse("20"));
                int albumCount = Integer.parseInt(
                    request.queryParam("albumCount").orElse("20"));
                int songCount = Integer.parseInt(
                    request.queryParam("songCount").orElse("20"));
                yield subsonicService.search2(query, artistCount, albumCount, songCount);
            }
            case "getPlaylists" -> subsonicService.getPlaylists();
            case "getPlaylist" -> {
                String id = request.queryParam("id").orElse("");
                yield subsonicService.getPlaylist(id);
            }
            case "createPlaylist" -> {
                String id = request.queryParam("playlistId").orElse("");
                String name = request.queryParam("name").orElse("");
                List<String> songIds =
                    request.queryParams().getOrDefault("songId", List.of());
                yield subsonicService.createPlaylist(id, name, songIds);
            }
            case "deletePlaylist" -> {
                String id = request.queryParam("id").orElse("");
                yield subsonicService.deletePlaylist(id);
            }
            case "scrobble" -> {
                String id = request.queryParam("id").orElse("");
                long time = Long.parseLong(request.queryParam("time").orElse("0"));
                boolean submission = Boolean.parseBoolean(
                    request.queryParam("submission").orElse("true"));
                yield subsonicService.scrobble(id, time, submission);
            }
            default -> Mono.just(
                SubsonicResponseBody.builder()
                    .status("failed")
                    .version("1.16.0").type("ikaros").serverVersion("1.2.1")
                    .error(SubsonicResponse.Error.builder()
                        .code(70).message("未知方法: " + method).build())
                    .build()
            );
        };
        return bodyMono.flatMap(body -> formatResponse(body, "json"));
    }

    private Mono<ServerResponse> handleStream(String id) {
        return subsonicService.stream(id)
            .flatMap(resource -> ServerResponse.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .bodyValue(resource))
            .onErrorResume(e -> {
                SubsonicResponseBody errBody = SubsonicResponseBody.builder()
                    .status("failed").version("1.16.0")
                    .type("ikaros").serverVersion("1.2.1")
                    .error(SubsonicResponse.Error.builder()
                        .code(70).message("音频流不可用: " + e.getMessage()).build())
                    .build();
                return formatResponse(errBody, "json");
            });
    }

    private Mono<ServerResponse> handleCoverArt(String id, int size) {
        return subsonicService.getCoverArt(id, size)
            .flatMap(resource -> ServerResponse.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .bodyValue(resource))
            .onErrorResume(e -> {
                SubsonicResponseBody errBody = SubsonicResponseBody.builder()
                    .status("failed").version("1.16.0")
                    .type("ikaros").serverVersion("1.2.1")
                    .error(SubsonicResponse.Error.builder()
                        .code(70).message("封面不可用: " + e.getMessage()).build())
                    .build();
                return formatResponse(errBody, "json");
            });
    }

    private Mono<ServerResponse> formatResponse(SubsonicResponseBody body, String format) {
        SubsonicResponse response = SubsonicResponse.builder().body(body).build();
        String json = run.ikaros.server.infra.utils.JsonUtils.obj2Json(response);
        if (json == null) {
            json = "{\"subsonic-response\":{\"status\":\"failed\","
                + "\"version\":\"1.16.0\",\"type\":\"ikaros\","
                + "\"serverVersion\":\"1.2.1\"}}";
        }
        return ServerResponse.ok()
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(json);
    }

    private String extractMethod(String path) {
        // /rest/ping.view → ping
        // /rest/getArtists → getArtists
        String name = path.substring("/rest/".length());
        if (name.endsWith(".view")) {
            name = name.substring(0, name.length() - ".view".length());
        }
        return name;
    }
}
