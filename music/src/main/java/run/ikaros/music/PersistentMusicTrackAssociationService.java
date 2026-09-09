package run.ikaros.music;

import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.ConflictException;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.CreateResourceRequest;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;

@Service
public class PersistentMusicTrackAssociationService implements MusicTrackAssociationService {
    private final MusicTrackRepository tracks;
    private final MusicMetadataCandidateRepository candidates;
    private final MusicTrackAssociationRepository associations;
    private final MusicTrackArtistRepository trackArtists;
    private final MusicArtistRepository artists;
    private final MusicAlbumRepository albums;
    private final MusicEditionRepository editions;
    private final MusicDiscRepository discs;
    private final MusicTrackMembershipRepository memberships;
    private final ResourceService resources;
    private final TransactionalOperator transaction;

    public PersistentMusicTrackAssociationService(MusicTrackRepository tracks, MusicMetadataCandidateRepository candidates,
            MusicTrackAssociationRepository associations, MusicTrackArtistRepository trackArtists,
            MusicArtistRepository artists, MusicAlbumRepository albums, MusicEditionRepository editions,
            MusicDiscRepository discs, MusicTrackMembershipRepository memberships, ResourceService resources,
            TransactionalOperator transaction) {
        this.tracks=tracks; this.candidates=candidates; this.associations=associations; this.trackArtists=trackArtists;
        this.artists=artists; this.albums=albums; this.editions=editions; this.discs=discs; this.memberships=memberships;
        this.resources=resources; this.transaction=transaction;
    }

    @Override public Mono<MusicTrackAssociationView> create(UUID ownerId, UUID trackId,
            CreateMusicTrackAssociationRequest request) {
        return tracks.findById(trackId).filter(t -> t.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Track 不存在或无权访问")))
            .flatMap(track -> candidates.findById(request.candidateId())
                .filter(c -> c.ownerId().equals(ownerId) && c.trackId().equals(trackId))
                .switchIfEmpty(Mono.error(new NotFoundException("Metadata Candidate 不存在或不属于该 Track")))
                .flatMap(candidate -> associations.findByOwnerIdAndTrackIdAndCandidateId(ownerId, trackId, candidate.id())
                    .map(existing -> view(existing, candidate))
                    .switchIfEmpty(Mono.defer(() -> createNew(ownerId, trackId, candidate)))))
            .as(transaction::transactional);
    }

    private Mono<MusicTrackAssociationView> createNew(UUID ownerId, UUID trackId,
            MusicMetadataCandidateEntity candidate) {
        if (blank(candidate.artist()) || blank(candidate.album())) {
            return Mono.error(new ConflictException("候选元数据缺少艺术家或专辑"));
        }
        return resources.create(ownerId, new CreateResourceRequest(ResourceType.MUSIC, candidate.artist().trim(), "und"))
            .flatMap(artistResource -> resources.create(ownerId,
                new CreateResourceRequest(ResourceType.MUSIC, candidate.album().trim(), "und"))
                .flatMap(albumResource -> saveArtist(ownerId, trackId, candidate, artistResource.id(), albumResource.id())));
    }

    private Mono<MusicTrackAssociationView> saveArtist(UUID ownerId, UUID trackId,
            MusicMetadataCandidateEntity candidate, UUID artistResourceId, UUID albumResourceId) {
        return artists.save(new MusicArtistEntity(null, ownerId, artistResourceId, "PRIMARY",
                candidate.artist().trim(), null, null))
            .flatMap(artist -> saveAlbum(ownerId, trackId, candidate, artist, albumResourceId));
    }

    private Mono<MusicTrackAssociationView> saveAlbum(UUID ownerId, UUID trackId,
            MusicMetadataCandidateEntity candidate, MusicArtistEntity artist, UUID albumResourceId) {
        return albums.save(new MusicAlbumEntity(null, ownerId, albumResourceId, "ALBUM", null, null))
            .flatMap(album -> saveEdition(ownerId, trackId, candidate, artist, album));
    }

    private Mono<MusicTrackAssociationView> saveEdition(UUID ownerId, UUID trackId,
            MusicMetadataCandidateEntity candidate, MusicArtistEntity artist, MusicAlbumEntity album) {
        return editions.save(new MusicEditionEntity(null, ownerId, album.id(), "Embedded metadata",
                null, null, null, null, null, null)).flatMap(edition -> saveDisc(ownerId, trackId, candidate, artist, album, edition));
    }

    private Mono<MusicTrackAssociationView> saveDisc(UUID ownerId, UUID trackId,
            MusicMetadataCandidateEntity candidate, MusicArtistEntity artist, MusicAlbumEntity album,
            MusicEditionEntity edition) {
        int discNumber = candidate.discNumber() == null ? 1 : candidate.discNumber();
        return discs.save(new MusicDiscEntity(null, edition.id(), discNumber, null, "DIGITAL", 1))
            .flatMap(disc -> saveMembership(ownerId, trackId, candidate, artist, album, edition, disc));
    }

    private Mono<MusicTrackAssociationView> saveMembership(UUID ownerId, UUID trackId,
            MusicMetadataCandidateEntity candidate, MusicArtistEntity artist, MusicAlbumEntity album,
            MusicEditionEntity edition, MusicDiscEntity disc) {
        int trackNumber = candidate.trackNumber() == null ? 1 : candidate.trackNumber();
        return memberships.save(new MusicTrackMembershipEntity(null, edition.id(), disc.id(), trackId,
                trackNumber, trackNumber - 1, candidate.title(), null, false))
            .flatMap(membership -> saveAssociation(ownerId, trackId, candidate, artist, album, edition, disc, membership));
    }

    private Mono<MusicTrackAssociationView> saveAssociation(UUID ownerId, UUID trackId,
            MusicMetadataCandidateEntity candidate, MusicArtistEntity artist, MusicAlbumEntity album,
            MusicEditionEntity edition, MusicDiscEntity disc, MusicTrackMembershipEntity membership) {
        return trackArtists.save(new MusicTrackArtistEntity(null, ownerId, trackId, artist.id(), "PRIMARY", 0))
            .then(associations.save(new MusicTrackAssociationEntity(null, ownerId, trackId, candidate.id(), artist.id(),
                album.id(), edition.id(), disc.id(), membership.id(), Instant.now(), null)))
            .map(saved -> view(saved, candidate));
    }

    @Override public Flux<MusicTrackAssociationView> list(UUID ownerId, UUID trackId) {
        return tracks.findById(trackId).filter(t -> t.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Track 不存在或无权访问")))
            .thenMany(associations.findAllByOwnerIdAndTrackId(ownerId, trackId)
                .flatMap(a -> candidates.findById(a.candidateId()).map(c -> view(a, c))));
    }

    private MusicTrackAssociationView view(MusicTrackAssociationEntity association,
            MusicMetadataCandidateEntity candidate) {
        return new MusicTrackAssociationView(association.id(), association.trackId(), association.candidateId(),
            association.artistId(), association.albumId(), association.editionId(), association.membershipId(),
            candidate.artist(), candidate.album(), "Embedded metadata", candidate.trackNumber(), association.createdAt());
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }
}
