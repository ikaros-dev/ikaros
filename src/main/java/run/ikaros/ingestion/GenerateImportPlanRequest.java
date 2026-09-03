package run.ikaros.ingestion;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
public record GenerateImportPlanRequest(@NotNull Boolean dryRun, Map<String, Object> policySnapshot) { }
