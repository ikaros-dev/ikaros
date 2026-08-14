package run.ikaros.api.core.binding;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jspecify.annotations.Nullable;
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
    private @Nullable UUID directoryId;
    private @Nullable String directoryName;
    private @Nullable String cleanName;
    private @Nullable String keyword;
    @Default
    private List<String> bracketTags = new ArrayList<>();

    private @Nullable SubjectSyncPlatform platform;
    private @Nullable String platformId;

    private @Nullable UUID subjectId;
    private @Nullable Subject subject;
    private @Nullable SubjectSync subjectSync;

    @Default
    private List<Attachment> childAttachments = new ArrayList<>();
    @Default
    private List<Attachment> spSubdirectoryAttachments = new ArrayList<>();

    @Default
    private List<Episode> createdEpisodes = new ArrayList<>();
    @Default
    private List<Tag> createdTags = new ArrayList<>();
    @Default
    private List<AttachmentReference> createdAttachmentRefs = new ArrayList<>();

    @Default
    private Map<String, DirectoryBindingStepStatus> stepResults = new HashMap<>();
    @Default
    private Map<String, String> stepErrors = new HashMap<>();
    @Default
    private Map<String, Object> parameters = new HashMap<>();

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
