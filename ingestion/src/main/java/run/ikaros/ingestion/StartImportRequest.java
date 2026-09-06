package run.ikaros.ingestion;
import jakarta.validation.constraints.NotNull;
public record StartImportRequest(@NotNull Long expectedPlanVersion) { }
