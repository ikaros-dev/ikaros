package run.ikaros.music;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class PersistentMusicPlaylistServiceTest {
    @Test void updatesOwnedPlaylist() {
        UUID owner = UUID.randomUUID(); UUID id = UUID.randomUUID(); Instant now = Instant.now();
        MusicPlaylistRepository playlists = Mockito.mock(MusicPlaylistRepository.class);
        MusicPlaylistEntity current = new MusicPlaylistEntity(id, owner, "Old", null, now, now, 2L);
        MusicPlaylistEntity saved = new MusicPlaylistEntity(id, owner, "New", "desc", now, now, 3L);
        when(playlists.findById(id)).thenReturn(Mono.just(current)); when(playlists.save(any())).thenReturn(Mono.just(saved));
        StepVerifier.create(new PersistentMusicPlaylistService(playlists, Mockito.mock(MusicPlaylistEntryRepository.class), Mockito.mock(MusicTrackRepository.class)).update(owner, id, new UpdateMusicPlaylistRequest("New", "desc", 2L)))
            .expectNextMatches(view -> view.name().equals("New") && view.version() == 3L).verifyComplete();
    }

    @Test void rejectsStalePlaylistVersion() {
        UUID owner = UUID.randomUUID(); UUID id = UUID.randomUUID(); Instant now = Instant.now();
        MusicPlaylistRepository playlists = Mockito.mock(MusicPlaylistRepository.class);
        when(playlists.findById(id)).thenReturn(Mono.just(new MusicPlaylistEntity(id, owner, "Old", null, now, now, 2L)));
        StepVerifier.create(new PersistentMusicPlaylistService(playlists, Mockito.mock(MusicPlaylistEntryRepository.class), Mockito.mock(MusicTrackRepository.class)).update(owner, id, new UpdateMusicPlaylistRequest("New", null, 1L)))
            .expectError(run.ikaros.common.ConflictException.class).verify();
    }

    @Test void rejectsForeignPlaylist() {
        UUID owner = UUID.randomUUID(); UUID id = UUID.randomUUID(); Instant now = Instant.now();
        MusicPlaylistRepository playlists = Mockito.mock(MusicPlaylistRepository.class);
        when(playlists.findById(id)).thenReturn(Mono.just(new MusicPlaylistEntity(id, UUID.randomUUID(), "Old", null, now, now, 0L)));
        StepVerifier.create(new PersistentMusicPlaylistService(playlists, Mockito.mock(MusicPlaylistEntryRepository.class), Mockito.mock(MusicTrackRepository.class)).update(owner, id, new UpdateMusicPlaylistRequest("New", null, 0L)))
            .expectError(run.ikaros.common.NotFoundException.class).verify();
    }

    @Test void appendsOwnedTrackToPlaylist() {
        UUID owner = UUID.randomUUID(); UUID id = UUID.randomUUID(); UUID trackId = UUID.randomUUID(); Instant now = Instant.now();
        MusicPlaylistRepository playlists = Mockito.mock(MusicPlaylistRepository.class); MusicPlaylistEntryRepository entries = Mockito.mock(MusicPlaylistEntryRepository.class); MusicTrackRepository tracks = Mockito.mock(MusicTrackRepository.class);
        when(playlists.findById(id)).thenReturn(Mono.just(new MusicPlaylistEntity(id, owner, "List", null, now, now, 0L)));
        when(tracks.findById(trackId)).thenReturn(Mono.just(new MusicTrackEntity(trackId, owner, UUID.randomUUID(), "Song", 1000L, null, false, 0L)));
        when(entries.findAllByPlaylistIdOrderByPositionAsc(id)).thenReturn(Flux.empty());
        when(entries.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        StepVerifier.create(new PersistentMusicPlaylistService(playlists, entries, tracks).add(owner, id, new AddMusicPlaylistEntryRequest(trackId)))
            .expectNextMatches(view -> view.trackId().equals(trackId) && view.position() == 0).verifyComplete();
    }

    @Test void rejectsTrackOwnedByAnotherUser() {
        UUID owner = UUID.randomUUID(); UUID id = UUID.randomUUID(); UUID trackId = UUID.randomUUID(); Instant now = Instant.now();
        MusicPlaylistRepository playlists = Mockito.mock(MusicPlaylistRepository.class); MusicTrackRepository tracks = Mockito.mock(MusicTrackRepository.class);
        when(playlists.findById(id)).thenReturn(Mono.just(new MusicPlaylistEntity(id, owner, "List", null, now, now, 0L)));
        when(tracks.findById(trackId)).thenReturn(Mono.just(new MusicTrackEntity(trackId, UUID.randomUUID(), UUID.randomUUID(), "Song", 1000L, null, false, 0L)));
        StepVerifier.create(new PersistentMusicPlaylistService(playlists, Mockito.mock(MusicPlaylistEntryRepository.class), tracks).add(owner, id, new AddMusicPlaylistEntryRequest(trackId)))
            .expectError(run.ikaros.common.NotFoundException.class).verify();
    }

    @Test void reordersOwnedPlaylistEntriesAndBumpsVersion() {
        UUID owner = UUID.randomUUID(); UUID playlistId = UUID.randomUUID(); UUID first = UUID.randomUUID(); UUID second = UUID.randomUUID(); Instant now = Instant.now();
        MusicPlaylistRepository playlists = Mockito.mock(MusicPlaylistRepository.class); MusicPlaylistEntryRepository entries = Mockito.mock(MusicPlaylistEntryRepository.class);
        when(playlists.findById(playlistId)).thenReturn(Mono.just(new MusicPlaylistEntity(playlistId, owner, "List", null, now, now, 2L)));
        when(entries.findAllByPlaylistIdOrderByPositionAsc(playlistId)).thenReturn(Flux.just(new MusicPlaylistEntryEntity(first, playlistId, UUID.randomUUID(), 0, now), new MusicPlaylistEntryEntity(second, playlistId, UUID.randomUUID(), 1, now)));
        when(entries.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
        when(playlists.save(any())).thenReturn(Mono.just(new MusicPlaylistEntity(playlistId, owner, "List", null, now, now, 3L)));
        StepVerifier.create(new PersistentMusicPlaylistService(playlists, entries, Mockito.mock(MusicTrackRepository.class)).reorder(owner, playlistId, new ReorderMusicPlaylistRequest(java.util.List.of(second, first)), 2L))
            .expectNextMatches(view -> view.version() == 3L).verifyComplete();
    }

    @Test void rejectsPlaylistReorderWithStaleVersion() {
        UUID owner = UUID.randomUUID(); UUID playlistId = UUID.randomUUID(); Instant now = Instant.now(); MusicPlaylistRepository playlists = Mockito.mock(MusicPlaylistRepository.class);
        when(playlists.findById(playlistId)).thenReturn(Mono.just(new MusicPlaylistEntity(playlistId, owner, "List", null, now, now, 2L)));
        StepVerifier.create(new PersistentMusicPlaylistService(playlists, Mockito.mock(MusicPlaylistEntryRepository.class), Mockito.mock(MusicTrackRepository.class)).reorder(owner, playlistId, new ReorderMusicPlaylistRequest(java.util.List.of(UUID.randomUUID())), 1L))
            .expectError(run.ikaros.common.ConflictException.class).verify();
    }
}
