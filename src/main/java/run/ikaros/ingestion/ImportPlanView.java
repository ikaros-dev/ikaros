package run.ikaros.ingestion;
import java.time.Instant;
import java.util.UUID;
public record ImportPlanView(UUID id, UUID scanRunId, boolean dryRun, String status, Instant generatedAt, long itemCount) { }
