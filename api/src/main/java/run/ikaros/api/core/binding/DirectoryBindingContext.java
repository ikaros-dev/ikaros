package run.ikaros.api.core.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.core.subject.Episode;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.SubjectSync;
import run.ikaros.api.core.tag.Tag;
import run.ikaros.api.store.enums.SubjectSyncPlatform;

/**
 * Mutable state carried through the directory binding chain.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class DirectoryBindingContext {
    private UUID directoryId;
    private String directoryName;
    private String cleanName;
    private String keyword;
    private List<String> bracketTags;

    private SubjectSyncPlatform platform;
    private String platformId;

    private UUID subjectId;
    private Subject subject;
    private SubjectSync subjectSync;

    private List<Attachment> childAttachments;
    private List<Attachment> spSubdirectoryAttachments;

    private List<Episode> createdEpisodes;
    private List<Tag> createdTags;
    private List<AttachmentReference> createdAttachmentRefs;

    private Map<String, DirectoryBindingStepStatus> stepResults;
    private Map<String, String> stepErrors;
    private Map<String, Object> parameters;

    /**
     * Create a new context with initialized collections.
     */
    public static DirectoryBindingContext create(UUID directoryId, String directoryName,
                                                 SubjectSyncPlatform platform) {
        return DirectoryBindingContext.builder()
            .directoryId(directoryId)
            .directoryName(directoryName)
            .platform(platform)
            .bracketTags(new ArrayList<>())
            .childAttachments(new ArrayList<>())
            .spSubdirectoryAttachments(new ArrayList<>())
            .createdEpisodes(new ArrayList<>())
            .createdTags(new ArrayList<>())
            .createdAttachmentRefs(new ArrayList<>())
            .stepResults(new HashMap<>())
            .stepErrors(new HashMap<>())
            .parameters(new HashMap<>())
            .build();
    }
}
