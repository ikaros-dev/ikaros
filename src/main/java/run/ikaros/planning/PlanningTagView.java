package run.ikaros.planning;

import java.time.Instant;
import java.util.UUID;

public record PlanningTagView(UUID id, UUID ownerId, String name, String color, Instant createdAt) {}
