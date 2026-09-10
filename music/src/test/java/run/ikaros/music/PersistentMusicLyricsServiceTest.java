package run.ikaros.music;

import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.common.NotFoundException;

class PersistentMusicLyricsServiceTest {
    private final MusicTrackRepository tracks = Mockito.mock(MusicTrackRepository.class);
    private final MusicLyricsRepository lyrics = Mockito.mock(MusicLyricsRepository.class);
    private final PersistentMusicLyricsService service = new PersistentMusicLyricsService(tracks, lyrics);
    private final UUID owner = UUID.randomUUID();
    private final UUID trackId = UUID.randomUUID();

    @Test void listsOwnedLyricsInRepositoryOrder() {
        when(tracks.findById(trackId)).thenReturn(Flux.just(new MusicTrackEntity(trackId, owner, UUID.randomUUID(), "Song", 1000L, null, false, 0L)).next());
        when(lyrics.findAllByOwnerIdAndTrackIdOrderByCreatedAtDesc(owner, trackId)).thenReturn(Flux.just(new MusicLyricsEntity(UUID.randomUUID(), owner, trackId, "zh-CN", "TIMED_LINE", "[00:01.00]你好", "[]", "ID3", "embedded", 0.9, Instant.now(), 0L)));
        StepVerifier.create(service.list(owner, trackId)).assertNext(value -> { assertEquals(trackId, value.trackId()); assertEquals("TIMED_LINE", value.type()); }).verifyComplete();
    }

    @Test void returnsEmptyWhenTrackHasNoLyrics() {
        when(tracks.findById(trackId)).thenReturn(Flux.just(new MusicTrackEntity(trackId, owner, UUID.randomUUID(), "Song", 1000L, null, false, 0L)).next());
        when(lyrics.findAllByOwnerIdAndTrackIdOrderByCreatedAtDesc(owner, trackId)).thenReturn(Flux.empty());
        StepVerifier.create(service.list(owner, trackId)).verifyComplete();
    }

    @Test void rejectsMissingOrForeignTrack() {
        when(tracks.findById(trackId)).thenReturn(Mono.empty());
        StepVerifier.create(service.list(owner, trackId)).expectError(NotFoundException.class).verify();
    }
}
