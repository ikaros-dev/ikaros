package run.ikaros.music;
import java.time.Instant; import java.util.UUID; import org.springframework.stereotype.Service; import reactor.core.publisher.Flux; import reactor.core.publisher.Mono; import run.ikaros.common.NotFoundException;
@Service public class PersistentMusicPlaylistService implements MusicPlaylistService {
    private final MusicPlaylistRepository playlists; private final MusicPlaylistEntryRepository entries; private final MusicTrackRepository tracks;
    public PersistentMusicPlaylistService(MusicPlaylistRepository playlists, MusicPlaylistEntryRepository entries, MusicTrackRepository tracks) { this.playlists=playlists; this.entries=entries; this.tracks=tracks; }
    @Override public Mono<MusicPlaylistView> create(UUID ownerId, CreateMusicPlaylistRequest r) { Instant now=Instant.now(); return playlists.save(new MusicPlaylistEntity(null,ownerId,r.name().trim(),r.description(),now,now,null)).map(this::view); }
    @Override public Flux<MusicPlaylistView> list(UUID ownerId) { return playlists.findAllByOwnerIdOrderByUpdatedAtDesc(ownerId).take(100).map(this::view); }
    @Override public Flux<MusicPlaylistEntryView> entries(UUID ownerId, UUID playlistId) { return ownedPlaylist(ownerId,playlistId).flatMapMany(p->entries.findAllByPlaylistIdOrderByPositionAsc(playlistId).take(100).map(this::entryView)); }
    @Override public Mono<MusicPlaylistEntryView> add(UUID ownerId, UUID playlistId, AddMusicPlaylistEntryRequest r) { return ownedPlaylist(ownerId,playlistId).then(tracks.findById(r.trackId()).filter(t->t.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Track 不存在或无权访问")))).then(entries.findAllByPlaylistIdOrderByPositionAsc(playlistId).collectList()).flatMap(all->entries.save(new MusicPlaylistEntryEntity(null,playlistId,r.trackId(),all.size(),Instant.now()))).map(this::entryView); }
    @Override public Mono<Void> remove(UUID ownerId, UUID entryId) { return entries.findById(entryId).switchIfEmpty(Mono.error(new NotFoundException("Playlist Entry 不存在"))).flatMap(e->ownedPlaylist(ownerId,e.playlistId()).then(entries.delete(e))); }
    private Mono<MusicPlaylistEntity> ownedPlaylist(UUID ownerId,UUID id){return playlists.findById(id).filter(p->p.ownerId().equals(ownerId)).switchIfEmpty(Mono.error(new NotFoundException("Playlist 不存在或无权访问")));}
    private MusicPlaylistView view(MusicPlaylistEntity e){return new MusicPlaylistView(e.id(),e.name(),e.description(),e.createdAt(),e.updatedAt());}
    private MusicPlaylistEntryView entryView(MusicPlaylistEntryEntity e){return new MusicPlaylistEntryView(e.id(),e.playlistId(),e.trackId(),e.position(),e.addedAt());}
}
