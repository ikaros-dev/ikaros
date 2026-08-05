package run.ikaros.server.core.binding;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.binding.LocalMediaMode;
import run.ikaros.api.core.binding.LocalScanItem;
import run.ikaros.api.core.binding.LocalScanPreview;
import run.ikaros.api.core.binding.LocalScanPreviewRequest;
import run.ikaros.api.core.binding.MediaPhysicalType;
import run.ikaros.api.core.binding.MediaRole;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.server.core.attachment.extension.LocalAttachmentPathValidator;
import run.ikaros.server.store.entity.AttachmentEntity;
import run.ikaros.server.store.repository.AttachmentRepository;

/** 从受信任的本地附件树递归读取文件并生成媒体扫描预览。 */
@Slf4j
@Service
public class DefaultLocalMediaScanner implements LocalMediaScanner {

    /** 参与附件树查询的仓储。 */
    private final AttachmentRepository attachmentRepository;
    /** 本地附件真实路径的访问边界。 */
    private final LocalAttachmentPathValidator pathValidator;
    /** 内嵌媒体轨道探测服务。 */
    private final MediaTrackProbeService mediaTrackProbeService;

    public DefaultLocalMediaScanner(AttachmentRepository attachmentRepository,
                                    LocalAttachmentPathValidator pathValidator,
                                    MediaTrackProbeService mediaTrackProbeService) {
        this.attachmentRepository = attachmentRepository;
        this.pathValidator = pathValidator;
        this.mediaTrackProbeService = mediaTrackProbeService;
    }

    @Override
    public Mono<LocalScanPreview> scan(LocalScanPreviewRequest request) {
        if (request == null || request.getDirectoryId() == null || request.getMode() == null) {
            return Mono.error(new IllegalArgumentException("目录附件和扫描模式不能为空"));
        }
        return attachmentRepository.findById(request.getDirectoryId())
            .switchIfEmpty(Mono.error(new IllegalArgumentException("待扫描目录附件不存在")))
            .filter(this::isDirectory)
            .switchIfEmpty(Mono.error(new IllegalArgumentException("待扫描附件不是目录")))
            .flatMap(directory -> pathValidator.validate(
                directory.getDriverId(), directory.getFsPath())
                .flatMap(rootPath -> scanDirectory(directory, rootPath, request.getMode())))
            .map(items -> LocalScanPreview.builder()
                .directoryId(request.getDirectoryId())
                .mode(request.getMode())
                .items(items)
                .build());
    }

    private Mono<List<LocalScanItem>> scanDirectory(AttachmentEntity directory, Path rootPath,
                                                     LocalMediaMode mode) {
        return descendants(directory.getId())
            .filter(attachment -> !isDirectory(attachment))
            .concatMap(attachment -> validateAttachment(rootPath, attachment))
            .collectList()
            .map(attachments -> toDrafts(attachments, mode))
            .map(this::mergeVobSubCandidates)
            .map(this::associateCandidates)
            .flatMap(drafts -> Flux.fromIterable(drafts)
                .concatMap(this::toScanItem)
                .collectList())
            .doOnSuccess(items -> log.info("本地媒体扫描完成: directoryId={}, itemCount={}",
                directory.getId(), items.size()));
    }

    private Flux<AttachmentEntity> descendants(UUID directoryId) {
        return attachmentRepository.findAllByParentId(directoryId)
            .concatMap(attachment -> isDirectory(attachment)
                ? descendants(attachment.getId())
                : Mono.just(attachment));
    }

    private Mono<ScannedAttachment> validateAttachment(Path rootPath, AttachmentEntity attachment) {
        return pathValidator.validate(attachment.getDriverId(), attachment.getFsPath())
            .filter(path -> path.startsWith(rootPath))
            .switchIfEmpty(Mono.error(new IllegalArgumentException("附件路径不属于扫描目录")))
            .map(path -> new ScannedAttachment(attachment, path, relativePath(rootPath, path)))
            .onErrorResume(error -> {
                log.debug("跳过无效本地附件: attachmentId={}", attachment.getId(), error);
                return Mono.empty();
            });
    }

    private List<ScanDraft> toDrafts(List<ScannedAttachment> attachments, LocalMediaMode mode) {
        return attachments.stream()
            .map(attachment -> toDraft(attachment, mode))
            .sorted(Comparator.comparing(ScanDraft::relativePath, this::compareNaturalPath))
            .toList();
    }

    private ScanDraft toDraft(ScannedAttachment scannedAttachment, LocalMediaMode mode) {
        String extension = extensionOf(scannedAttachment.attachment().getName());
        MediaPhysicalType physicalType = physicalTypeOf(extension, mode);
        return new ScanDraft(scannedAttachment, extension, physicalType,
            roleFor(physicalType, mode), null);
    }

    private List<ScanDraft> mergeVobSubCandidates(List<ScanDraft> drafts) {
        return drafts.stream()
            .filter(draft -> !draft.extension().equals(".idx")
                || drafts.stream().noneMatch(candidate -> candidate.extension().equals(".sub")
                    && candidate.vobSubKey().equals(draft.vobSubKey())))
            .toList();
    }

    private List<ScanDraft> associateCandidates(List<ScanDraft> drafts) {
        Map<String, List<ScanDraft>> primaryByKey = new LinkedHashMap<>();
        for (ScanDraft draft : drafts) {
            if (draft.role() == MediaRole.PRIMARY) {
                primaryByKey.computeIfAbsent(
                    draft.associationKey(), ignored -> new ArrayList<>()).add(draft);
            }
        }
        return drafts.stream().map(draft -> {
            if (draft.role() != MediaRole.PENDING_CONFIRMATION) {
                return draft;
            }
            List<ScanDraft> primaries = primaryByKey.getOrDefault(
                draft.associationKey(), List.of());
            return primaries.size() == 1
                ? draft.withAssociation(MediaRole.AUTO_ASSOCIATED,
                    primaries.get(0).attachment().getId())
                : draft;
        }).toList();
    }

    private Mono<LocalScanItem> toScanItem(ScanDraft draft) {
        Mono<MediaTrackProbeService.ProbeResult> probeResult =
            draft.physicalType() == MediaPhysicalType.VIDEO
                ? mediaTrackProbeService.probe(draft.realPath())
                : Mono.just(MediaTrackProbeService.ProbeResult.success(List.of()));
        return probeResult.map(result -> LocalScanItem.builder()
            .attachmentId(draft.attachment().getId())
            .relativePath(draft.relativePath())
            .physicalType(draft.physicalType())
            .role(draft.role())
            .displayMetadata(displayMetadata(draft))
            .candidatePrimaryAttachmentId(draft.candidatePrimaryAttachmentId())
            .tracks(result.tracks())
            .probeFailureReason(result.failureReason())
            .build())
            .onErrorResume(error -> Mono.just(LocalScanItem.builder()
                .attachmentId(draft.attachment().getId())
                .relativePath(draft.relativePath())
                .physicalType(draft.physicalType())
                .role(draft.role())
                .displayMetadata(displayMetadata(draft))
                .candidatePrimaryAttachmentId(draft.candidatePrimaryAttachmentId())
                .tracks(List.of())
                .probeFailureReason("媒体轨道探测失败")
                .build()));
    }

    private Map<String, String> displayMetadata(ScanDraft draft) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("extension", draft.extension());
        metadata.put("name", filenameWithoutExtension(draft.attachment().getName()));
        return metadata;
    }

    private MediaRole roleFor(MediaPhysicalType physicalType, LocalMediaMode mode) {
        if (physicalType == MediaPhysicalType.UNKNOWN) {
            return MediaRole.UNKNOWN;
        }
        return switch (mode) {
            case EPISODE -> physicalType == MediaPhysicalType.VIDEO ? MediaRole.PRIMARY
                : physicalType == MediaPhysicalType.AUDIO
                    || physicalType == MediaPhysicalType.SUBTITLE
                    || physicalType == MediaPhysicalType.LYRICS
                    ? MediaRole.PENDING_CONFIRMATION
                    : MediaRole.UNASSOCIATED;
            case AUDIO -> physicalType == MediaPhysicalType.AUDIO ? MediaRole.PRIMARY
                : physicalType == MediaPhysicalType.LYRICS ? MediaRole.PENDING_CONFIRMATION
                : MediaRole.UNASSOCIATED;
            case IMAGE -> physicalType == MediaPhysicalType.IMAGE ? MediaRole.PRIMARY
                : MediaRole.UNASSOCIATED;
        };
    }

    private MediaPhysicalType physicalTypeOf(String extension, LocalMediaMode mode) {
        return switch (extension) {
            case ".mp4", ".m4v", ".mov", ".mkv", ".avi", ".webm", ".flv", ".ts", ".m2ts", ".wmv"
                -> MediaPhysicalType.VIDEO;
            case ".mp3", ".aac", ".m4a", ".flac", ".ogg", ".opus", ".wav", ".wma"
                -> MediaPhysicalType.AUDIO;
            case ".srt", ".ass", ".ssa", ".vtt", ".sub", ".idx" -> MediaPhysicalType.SUBTITLE;
            case ".ttml" -> mode == LocalMediaMode.EPISODE ? MediaPhysicalType.SUBTITLE
                : MediaPhysicalType.LYRICS;
            case ".lrc" -> MediaPhysicalType.LYRICS;
            case ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".avif"
                -> MediaPhysicalType.IMAGE;
            default -> MediaPhysicalType.UNKNOWN;
        };
    }

    private String extensionOf(String filename) {
        int extensionStart = filename == null ? -1 : filename.lastIndexOf('.');
        return extensionStart < 0 ? ""
            : filename.substring(extensionStart).toLowerCase(Locale.ROOT);
    }

    private static String filenameWithoutExtension(String filename) {
        int extensionStart = filename == null ? -1 : filename.lastIndexOf('.');
        return extensionStart < 0 ? filename : filename.substring(0, extensionStart);
    }

    private static String relativePath(Path rootPath, Path path) {
        return rootPath.relativize(path).toString().replace('\\', '/');
    }

    private boolean isDirectory(AttachmentEntity attachment) {
        return attachment.getType() == AttachmentType.Directory
            || attachment.getType() == AttachmentType.Driver_Directory;
    }

    private int compareNaturalPath(String left, String right) {
        int leftIndex = 0;
        int rightIndex = 0;
        while (leftIndex < left.length() && rightIndex < right.length()) {
            char leftCharacter = Character.toLowerCase(left.charAt(leftIndex));
            char rightCharacter = Character.toLowerCase(right.charAt(rightIndex));
            if (Character.isDigit(leftCharacter) && Character.isDigit(rightCharacter)) {
                int compared = compareNumericPart(left, right, leftIndex, rightIndex);
                if (compared != 0) {
                    return compared;
                }
                leftIndex = numericEnd(left, leftIndex);
                rightIndex = numericEnd(right, rightIndex);
                continue;
            }
            if (leftCharacter != rightCharacter) {
                return Character.compare(leftCharacter, rightCharacter);
            }
            leftIndex++;
            rightIndex++;
        }
        int result = Integer.compare(left.length(), right.length());
        return result != 0 ? result : left.compareTo(right);
    }

    private int compareNumericPart(String left, String right, int leftStart, int rightStart) {
        int leftEnd = numericEnd(left, leftStart);
        int rightEnd = numericEnd(right, rightStart);
        String leftNumber = stripLeadingZeros(left.substring(leftStart, leftEnd));
        String rightNumber = stripLeadingZeros(right.substring(rightStart, rightEnd));
        int lengthComparison = Integer.compare(leftNumber.length(), rightNumber.length());
        if (lengthComparison != 0) {
            return lengthComparison;
        }
        int valueComparison = leftNumber.compareTo(rightNumber);
        return valueComparison != 0 ? valueComparison : Integer.compare(leftEnd - leftStart,
            rightEnd - rightStart);
    }

    private int numericEnd(String value, int start) {
        int index = start;
        while (index < value.length() && Character.isDigit(value.charAt(index))) {
            index++;
        }
        return index;
    }

    private String stripLeadingZeros(String value) {
        int index = 0;
        while (index < value.length() - 1 && value.charAt(index) == '0') {
            index++;
        }
        return value.substring(index);
    }

    private record ScannedAttachment(AttachmentEntity attachment, Path realPath,
                                     String relativePath) {
    }

    private record ScanDraft(ScannedAttachment scannedAttachment, String extension,
                             MediaPhysicalType physicalType, MediaRole role,
                             UUID candidatePrimaryAttachmentId) {

        private AttachmentEntity attachment() {
            return scannedAttachment.attachment();
        }

        private Path realPath() {
            return scannedAttachment.realPath();
        }

        private String relativePath() {
            return scannedAttachment.relativePath();
        }

        private String associationKey() {
            String key = filenameWithoutExtension(attachment().getName());
            Matcher matcher = TRAILING_ASSOCIATION_TOKEN.matcher(key);
            while (matcher.find()) {
                key = key.substring(0, matcher.start());
                matcher = TRAILING_ASSOCIATION_TOKEN.matcher(key);
            }
            int filenameStart = relativePath().lastIndexOf('/');
            String parentPath = filenameStart < 0 ? ""
                : relativePath().substring(0, filenameStart + 1);
            return parentPath + key.toLowerCase(Locale.ROOT);
        }

        private String vobSubKey() {
            int extensionStart = relativePath().lastIndexOf('.');
            return (extensionStart < 0 ? relativePath()
                : relativePath().substring(0, extensionStart))
                .toLowerCase(Locale.ROOT);
        }

        private ScanDraft withAssociation(MediaRole role, UUID primaryAttachmentId) {
            return new ScanDraft(
                scannedAttachment, extension, physicalType, role, primaryAttachmentId);
        }
    }

    /** 可安全从匹配键末尾剔除的已知语义片段。 */
    private static final Pattern TRAILING_ASSOCIATION_TOKEN = Pattern.compile(
        "(?i)[._\\s-]+(?:zh|zho|chi|chs|cht|cn|en|eng|ja|jpn|ko|kor|default|forced|"
            + "commentary|signs|mono|stereo|\\d\\.\\d)$");
}
