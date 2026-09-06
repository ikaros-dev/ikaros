package run.ikaros.verification;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

/**
 * 发起当前用户安全验证挑战时接受的用途与关联目标。
 */
public record IssueVerificationRequest(@NotNull VerificationPurpose purpose, @Size(max = 255) String targetReference) {
}
