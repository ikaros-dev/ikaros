package run.ikaros.storage;

import run.ikaros.storage.api.*;

import jakarta.validation.constraints.NotNull;

public record StoragePlacementTieringRequest(@NotNull StorageTier targetTier) {}
