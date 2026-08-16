package run.ikaros.api.core.subsonic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;

/**
 * Subsonic API 统一的响应包装.
 * 所有 Subsonic API 响应都包装在 subsonic-response 对象中.
 *
 * @author Nekoli
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubsonicResponse {
    @JsonProperty("subsonic-response")
    private @Nullable SubsonicResponseBody body;

    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubsonicResponseBody {
        private @Nullable String status;  // "ok" | "failed"
        private @Nullable String version; // Subsonic API version
        private @Nullable String type;    // Server type
        @JsonProperty("serverVersion")
        private @Nullable String serverVersion;
        private @Nullable Error error;

        // Data containers
        private @Nullable Artists artists;
        private @Nullable Artist artist;
        private @Nullable AlbumList albumList;
        private @Nullable AlbumWithSongs album;
        private @Nullable SongChild song;
        private @Nullable SearchResult searchResult;
        private @Nullable Playlists playlists;
        private @Nullable PlaylistWithSongs playlist;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Error {
        private int code;
        private @Nullable String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Artists {
        private @Nullable List<ArtistIndex> index;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArtistIndex {
        private @Nullable String name;
        private @Nullable List<ArtistChild> artist;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArtistChild {
        private @Nullable String id;
        private @Nullable String name;
        @JsonProperty("albumCount")
        private int albumCount;
        @JsonProperty("coverArt")
        private @Nullable String coverArt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Artist {
        private @Nullable String id;
        private @Nullable String name;
        @JsonProperty("albumCount")
        private int albumCount;
        @JsonProperty("coverArt")
        private @Nullable String coverArt;
        private @Nullable List<AlbumChild> album;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlbumChild {
        private @Nullable String id;
        private @Nullable String name;
        private @Nullable String artist;
        @JsonProperty("coverArt")
        private @Nullable String coverArt;
        @JsonProperty("songCount")
        private int songCount;
        private @Nullable String duration;
        private @Nullable String created;
        private @Nullable String year;
        private @Nullable String genre;
        private @Nullable String parent;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlbumList {
        private @Nullable List<AlbumChild> album;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlbumWithSongs {
        private @Nullable String id;
        private @Nullable String name;
        private @Nullable String artist;
        @JsonProperty("coverArt")
        private @Nullable String coverArt;
        @JsonProperty("songCount")
        private int songCount;
        private @Nullable String duration;
        private @Nullable String created;
        private @Nullable String parent;
        private @Nullable List<SongChild> song;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SongChild {
        private @Nullable String id;
        private @Nullable String parent;
        private @Nullable String title;
        private @Nullable String artist;
        private @Nullable String album;
        private int track;
        private int duration;
        private @Nullable String contentType;
        @JsonProperty("coverArt")
        private @Nullable String coverArt;
        private long size;
        private @Nullable String path;
        private @Nullable String suffix;
        @JsonProperty("isDir")
        private boolean isDir;
        private int bitRate;
        private int year;
        private @Nullable String genre;
        private @Nullable String created;
        private @Nullable String albumId;
        private @Nullable String artistId;
        private @Nullable String type;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SearchResult {
        private @Nullable List<ArtistChild> artist;
        private @Nullable List<AlbumChild> album;
        private @Nullable List<SongChild> song;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Playlists {
        private @Nullable List<PlaylistChild> playlist;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlaylistChild {
        private @Nullable String id;
        private @Nullable String name;
        private @Nullable String comment;
        @JsonProperty("songCount")
        private int songCount;
        private @Nullable String duration;
        private @Nullable String created;
        private @Nullable String owner;
        @JsonProperty("public")
        private boolean isPublic;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlaylistWithSongs {
        private @Nullable String id;
        private @Nullable String name;
        private @Nullable String comment;
        @JsonProperty("songCount")
        private int songCount;
        private @Nullable String duration;
        private @Nullable String owner;
        @JsonProperty("public")
        private boolean isPublic;
        private @Nullable List<SongChild> entry;
    }
}
