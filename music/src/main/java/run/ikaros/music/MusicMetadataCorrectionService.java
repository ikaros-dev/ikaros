package run.ikaros.music;
import java.util.UUID; import reactor.core.publisher.Mono;
public interface MusicMetadataCorrectionService { Mono<MusicMetadataCandidateView> update(UUID ownerId,UUID candidateId,UpdateMusicMetadataCandidateRequest request,Long expectedVersion); }
