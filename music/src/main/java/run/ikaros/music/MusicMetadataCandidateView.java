package run.ikaros.music;
import java.time.Instant; import java.util.UUID;
public record MusicMetadataCandidateView(UUID id,UUID trackId,UUID attachmentId,String title,String artist,String album,String albumArtist,String isrc,String genre,Integer trackNumber,Integer discNumber,String releaseYear,String source,Instant createdAt,Instant updatedAt,Long version) {}
