package run.ikaros.music;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;

@Service
public class PersistentMusicLyricsService implements MusicLyricsService {
    private final MusicTrackRepository tracks;
    private final MusicLyricsRepository lyrics;

    public PersistentMusicLyricsService(MusicTrackRepository tracks, MusicLyricsRepository lyrics) {
        this.tracks = tracks;
        this.lyrics = lyrics;
    }

    @Override
    public Flux<MusicLyricsView> list(UUID ownerId, UUID trackId) {
        return tracks.findById(trackId).filter(track -> track.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Track 不存在或无权访问")))
            .thenMany(Flux.defer(() -> lyrics.findAllByOwnerIdAndTrackIdOrderByCreatedAtDesc(ownerId, trackId)
                .take(100).map(this::view)));
    }

    private MusicLyricsView view(MusicLyricsEntity value) {
        return new MusicLyricsView(value.id(), value.trackId(), value.language(), value.type(),
            value.content(), value.timingData(), value.source(), value.provenance(),
            value.confidence(), value.createdAt(), value.version());
    }
}
