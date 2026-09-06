package run.ikaros.backup;
import jakarta.validation.constraints.NotNull;
public record VerifyRestorePointRequest(@NotNull VerificationLevel level, @NotNull VerificationStatus status,
    String failureReason, long checkedObjects, long failedObjects) {}
