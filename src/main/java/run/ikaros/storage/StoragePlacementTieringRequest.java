package run.ikaros.storage;

import jakarta.validation.constraints.NotNull;

public record StoragePlacementTieringRequest(@NotNull StorageTier targetTier) {}
