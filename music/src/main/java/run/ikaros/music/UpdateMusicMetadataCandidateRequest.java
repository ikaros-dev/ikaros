package run.ikaros.music;
import jakarta.validation.constraints.Size;
public record UpdateMusicMetadataCandidateRequest(@Size(max=512) String title,@Size(max=512) String artist,@Size(max=512) String album,@Size(max=512) String albumArtist,@Size(max=64) String isrc,@Size(max=256) String genre,Integer trackNumber,Integer discNumber,@Size(max=16) String releaseYear) {}
