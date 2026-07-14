package run.ikaros.api.core.subsonic;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private SubsonicResponseBody body;

    @Data
    @Builder(toBuilder = true)
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SubsonicResponseBody {
        private String status;  // "ok" | "failed"
        private String version; // Subsonic API version
        private String type;    // Server type
        @JsonProperty("serverVersion")
        private String serverVersion;
        private Error error;

        // Data containers
        private Artists artists;
        private Artist artist;
        private AlbumList albumList;
        private AlbumWithSongs album;
        private SongChild song;
        private SearchResult searchResult;
        private Playlists playlists;
        private PlaylistWithSongs playlist;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Error {
        private int code;
        private String message;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Artists {
        private List<ArtistIndex> index;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArtistIndex {
        private String name;
        private List<ArtistChild> artist;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ArtistChild {
        private String id;
        private String name;
        @JsonProperty("albumCount")
        private int albumCount;
        @JsonProperty("coverArt")
        private String coverArt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Artist {
        private String id;
        private String name;
        @JsonProperty("albumCount")
        private int albumCount;
        @JsonProperty("coverArt")
        private String coverArt;
        private List<AlbumChild> album;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlbumChild {
        private String id;
        private String name;
        private String artist;
        @JsonProperty("coverArt")
        private String coverArt;
        @JsonProperty("songCount")
        private int songCount;
        private String duration;
        private String created;
        private String year;
        private String genre;
        private String parent;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlbumList {
        private List<AlbumChild> album;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AlbumWithSongs {
        private String id;
        private String name;
        private String artist;
        @JsonProperty("coverArt")
        private String coverArt;
        @JsonProperty("songCount")
        private int songCount;
        private String duration;
        private String created;
        private String parent;
        private List<SongChild> song;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SongChild {
        private String id;
        private String parent;
        private String title;
        private String artist;
        private String album;
        private int track;
        private int duration;
        private String contentType;
        @JsonProperty("coverArt")
        private String coverArt;
        private long size;
        private String path;
        private String suffix;
        @JsonProperty("isDir")
        private boolean isDir;
        private int bitRate;
        private int year;
        private String genre;
        private String created;
        private String albumId;
        private String artistId;
        private String type;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SearchResult {
        private List<ArtistChild> artist;
        private List<AlbumChild> album;
        private List<SongChild> song;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Playlists {
        private List<PlaylistChild> playlist;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlaylistChild {
        private String id;
        private String name;
        private String comment;
        @JsonProperty("songCount")
        private int songCount;
        private String duration;
        private String created;
        private String owner;
        @JsonProperty("public")
        private boolean isPublic;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlaylistWithSongs {
        private String id;
        private String name;
        private String comment;
        @JsonProperty("songCount")
        private int songCount;
        private String duration;
        private String owner;
        @JsonProperty("public")
        private boolean isPublic;
        private List<SongChild> entry;
    }
}
