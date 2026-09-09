package run.ikaros.music;
import java.time.Instant; import java.util.UUID;
public record MusicTrackAssociationView(UUID id,UUID trackId,UUID candidateId,UUID artistId,UUID albumId,UUID editionId,UUID membershipId,String artist,String album,String editionName,Integer trackNumber,Instant createdAt) {}
