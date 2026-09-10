package run.ikaros.game;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.resource.api.ResourceLifecycle;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;
import run.ikaros.resource.api.ResourceView;
import run.ikaros.storage.api.AttachmentReferenceQuery;

class PersistentGameArchiveServiceTest {
    @Test
    void createsGameResourceAndGameEntry() {
        UUID owner = UUID.randomUUID();
        UUID resourceId = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        ResourceService resources = mock(ResourceService.class);
        GameRepository games = mock(GameRepository.class);
        GamePlatformRepository platforms = mock(GamePlatformRepository.class);
        GameVersionRepository versions = mock(GameVersionRepository.class);
        GameAssetRepository assets = mock(GameAssetRepository.class);
        AttachmentReferenceQuery attachments = mock(AttachmentReferenceQuery.class);
        Instant now = Instant.now();
        when(resources.create(any(), any())).thenReturn(Mono.just(new ResourceView(resourceId, ResourceType.GAME, ResourceLifecycle.ACTIVE, List.of(), List.of(), now, now)));
        when(games.save(any())).thenReturn(Mono.just(new GameEntity(gameId, owner, resourceId, "PC", 0L)));

        StepVerifier.create(new PersistentGameArchiveService(resources, games, platforms, versions, assets, attachments)
                .createGame(owner, new CreateGameRequest("Test Game", "PC", "zh-CN")))
            .expectNextMatches(game -> game.id().equals(gameId) && game.resourceId().equals(resourceId) && game.gameKind().equals("PC"))
            .verifyComplete();
        verify(resources).create(any(), any());
        verify(games).save(any());
    }

    @Test
    void createsVersionWithoutOptionalPlatform() {
        UUID owner = UUID.randomUUID();
        UUID gameId = UUID.randomUUID();
        UUID versionId = UUID.randomUUID();
        ResourceService resources = mock(ResourceService.class);
        GameRepository games = mock(GameRepository.class);
        GamePlatformRepository platforms = mock(GamePlatformRepository.class);
        GameVersionRepository versions = mock(GameVersionRepository.class);
        GameAssetRepository assets = mock(GameAssetRepository.class);
        AttachmentReferenceQuery attachments = mock(AttachmentReferenceQuery.class);
        Instant now = Instant.now();
        when(games.findById(gameId)).thenReturn(Mono.just(new GameEntity(gameId, owner, UUID.randomUUID(), "PC", 0L)));
        when(versions.save(any())).thenReturn(Mono.just(new GameVersionEntity(versionId, owner, gameId, null, "1.0", now, 0L)));

        StepVerifier.create(new PersistentGameArchiveService(resources, games, platforms, versions, assets, attachments)
                .createVersion(owner, gameId, new CreateGameVersionRequest("1.0", null, now)))
            .expectNextMatches(version -> version.id().equals(versionId) && version.platformId() == null)
            .verifyComplete();
    }
}
