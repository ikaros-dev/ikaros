package run.ikaros.drive;
import jakarta.validation.constraints.PositiveOrZero;
public record AdvanceSyncCursorRequest(@PositiveOrZero long cursor) {}
