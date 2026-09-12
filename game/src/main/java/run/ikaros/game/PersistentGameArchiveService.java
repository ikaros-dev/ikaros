package run.ikaros.game;

import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
import run.ikaros.resource.api.CreateResourceRequest;
import run.ikaros.resource.api.ResourceService;
import run.ikaros.resource.api.ResourceType;
import run.ikaros.storage.api.AttachmentReferenceQuery;

@Service
public class PersistentGameArchiveService implements GameArchiveService {
    private final ResourceService resources;
    private final GameRepository games;
    private final GamePlatformRepository platforms;
    private final GameVersionRepository versions;
    private final GameAssetRepository assets;
    private final AttachmentReferenceQuery attachments;

    public PersistentGameArchiveService(ResourceService resources, GameRepository games,
        GamePlatformRepository platforms, GameVersionRepository versions, GameAssetRepository assets,
        AttachmentReferenceQuery attachments) {
        this.resources = resources;
        this.games = games;
        this.platforms = platforms;
        this.versions = versions;
        this.assets = assets;
        this.attachments = attachments;
    }

    @Override
    public Mono<GameView> createGame(UUID ownerId, CreateGameRequest request) {
        String locale = request.locale() == null || request.locale().isBlank() ? "en-US" : request.locale();
        return resources.create(ownerId, new CreateResourceRequest(ResourceType.GAME, request.title(), locale))
            .flatMap(resource -> games.save(new GameEntity(null, ownerId, resource.id(), request.gameKind(), null)))
            .map(this::gameView);
    }

    @Override
    public Flux<GameView> games(UUID ownerId) {
        return games.findAllByOwnerId(ownerId).take(100).map(this::gameView);
    }

    @Override
    public Mono<GamePlatformView> createPlatform(UUID ownerId, CreateGamePlatformRequest request) {
        return platforms.save(new GamePlatformEntity(null, ownerId, request.name().trim(), request.family(), request.architecture()))
            .map(this::platformView);
    }

    @Override
    public Flux<GamePlatformView> platforms(UUID ownerId) {
        return platforms.findAllByOwnerIdOrderByNameAsc(ownerId).take(100).map(this::platformView);
    }

    @Override
    public Mono<GameVersionView> createVersion(UUID ownerId, UUID gameId, CreateGameVersionRequest request) {
        return ownedGame(ownerId, gameId)
            .flatMap(game -> request.platformId() == null
                ? versions.save(new GameVersionEntity(null, ownerId, gameId, null, request.versionLabel(), request.releaseDate(), null))
                : platforms.findById(request.platformId()).filter(platform -> platform.ownerId().equals(ownerId))
                    .switchIfEmpty(Mono.error(new NotFoundException("Platform 不存在或无权访问")))
                    .flatMap(platform -> versions.save(new GameVersionEntity(null, ownerId, gameId, platform.id(), request.versionLabel(), request.releaseDate(), null))))
            .map(this::versionView);
    }

    @Override
    public Flux<GameVersionView> versions(UUID ownerId, UUID gameId) {
        return ownedGame(ownerId, gameId)
            .flatMapMany(game -> versions.findAllByOwnerIdAndGameId(ownerId, gameId).take(100).map(this::versionView));
    }

    @Override
    public Mono<GameAssetView> addAsset(UUID ownerId, UUID gameId, CreateGameAssetRequest request) {
        return ownedGame(ownerId, gameId)
            .flatMap(game -> attachments.requireActiveForResource(ownerId, game.resourceId(), request.attachmentId()))
            .then(assets.save(new GameAssetEntity(null, ownerId, gameId, request.versionId(), request.attachmentId(),
                request.category(), request.displayName(), request.checksumAlgorithm(), request.checksumValue(),
                GameAssetAvailability.AVAILABLE, null)))
            .map(this::assetView);
    }

    @Override
    public Flux<GameAssetView> assets(UUID ownerId, UUID gameId) {
        return ownedGame(ownerId, gameId)
            .flatMapMany(game -> assets.findAllByOwnerIdAndGameId(ownerId, gameId).take(100).map(this::assetView));
    }

    @Override
    public Mono<GameAssetView> availability(UUID ownerId, UUID assetId, UpdateGameAssetAvailabilityRequest request) {
        return assets.findById(assetId).filter(asset -> asset.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Game Asset 不存在或无权访问")))
            .flatMap(asset -> assets.save(new GameAssetEntity(asset.id(), asset.ownerId(), asset.gameId(), asset.versionId(),
                asset.attachmentId(), asset.category(), asset.displayName(), asset.checksumAlgorithm(), asset.checksumValue(),
                request.availability(), asset.entityVersion())))
            .map(this::assetView);
    }

    private Mono<GameEntity> ownedGame(UUID ownerId, UUID gameId) {
        return games.findById(gameId).filter(game -> game.ownerId().equals(ownerId))
            .switchIfEmpty(Mono.error(new NotFoundException("Game 不存在或无权访问")));
    }

    private GameView gameView(GameEntity entity) { return new GameView(entity.id(), entity.resourceId(), entity.gameKind()); }
    private GamePlatformView platformView(GamePlatformEntity entity) { return new GamePlatformView(entity.id(), entity.name(), entity.family(), entity.architecture()); }
    private GameVersionView versionView(GameVersionEntity entity) { return new GameVersionView(entity.id(), entity.gameId(), entity.platformId(), entity.versionLabel(), entity.releaseDate()); }
    private GameAssetView assetView(GameAssetEntity entity) { return new GameAssetView(entity.id(), entity.gameId(), entity.versionId(), entity.attachmentId(), entity.category(), entity.displayName(), entity.checksumAlgorithm(), entity.checksumValue(), entity.availability()); }
}
