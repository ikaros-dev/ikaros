package run.ikaros.document;

import java.time.Instant;
import java.util.UUID;

public record DocumentPresenceView(String clientId, UUID principalId, Instant lastSeenAt) {}
