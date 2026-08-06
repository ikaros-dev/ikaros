package run.ikaros.api.core.binding;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import run.ikaros.api.core.subject.Subject;

/** 本地扫描公共契约测试。 */
public class LocalScanContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void mediaEnums_ShouldContainSupportedValues() {
        assertThat(LocalMediaMode.values()).containsExactly(
            LocalMediaMode.EPISODE, LocalMediaMode.AUDIO, LocalMediaMode.IMAGE
        );
        assertThat(MediaPhysicalType.values()).containsExactly(
            MediaPhysicalType.VIDEO, MediaPhysicalType.AUDIO, MediaPhysicalType.SUBTITLE,
            MediaPhysicalType.LYRICS, MediaPhysicalType.IMAGE, MediaPhysicalType.UNKNOWN
        );
        assertThat(MediaRole.values()).containsExactly(
            MediaRole.PRIMARY, MediaRole.AUTO_ASSOCIATED, MediaRole.PENDING_CONFIRMATION,
            MediaRole.UNASSOCIATED, MediaRole.UNKNOWN
        );
    }

    @Test
    void localScanItem_ShouldSerializeCamelCaseFieldsAsSnakeCase() throws Exception {
        LocalScanItem item = LocalScanItem.builder()
            .attachmentId(UUID.fromString("efb501a8-f045-4ac6-a6a5-13a659dcce69"))
            .candidatePrimaryAttachmentId(UUID.fromString("493ce3f5-b711-4d4f-88d9-ec9bd53ca1e5"))
            .probeFailureReason("解析失败")
            .build();

        String json = objectMapper.writeValueAsString(item);

        assertThat(json).contains("attachment_id", "candidate_primary_attachment_id",
            "probe_failure_reason");
    }

    @Test
    void confirmRequest_ShouldRequireExactlyOneSubjectSelection() {
        var validator = Validation.buildDefaultValidatorFactory().getValidator();
        LocalScanConfirmRequest existingSubject = LocalScanConfirmRequest.builder()
            .subjectId(UUID.randomUUID())
            .build();
        LocalScanConfirmRequest newSubject = LocalScanConfirmRequest.builder()
            .subject(new Subject())
            .build();
        LocalScanConfirmRequest missingSubject = new LocalScanConfirmRequest();
        LocalScanConfirmRequest duplicatedSubject = LocalScanConfirmRequest.builder()
            .subjectId(UUID.randomUUID())
            .subject(new Subject())
            .build();

        assertThat(validator.validate(existingSubject)).isEmpty();
        assertThat(validator.validate(newSubject)).isEmpty();
        assertThat(validator.validate(missingSubject)).isNotEmpty();
        assertThat(validator.validate(duplicatedSubject)).isNotEmpty();
    }
}
