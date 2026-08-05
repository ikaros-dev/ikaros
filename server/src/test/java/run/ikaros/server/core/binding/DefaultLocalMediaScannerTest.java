package run.ikaros.server.core.binding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

/** 本地媒体扫描器分类与自动关联测试. */
class DefaultLocalMediaScannerTest {

    /** 附件树查询仓储. */
    @Mock
    private AttachmentRepository attachmentRepository;
    /** 本地路径校验器. */
    @Mock
    private LocalAttachmentPathValidator pathValidator;
    /** 内嵌轨道探测服务. */
    @Mock
    private MediaTrackProbeService mediaTrackProbeService;
    /** 待测试的本地媒体扫描器. */
    private LocalMediaScanner localMediaScanner;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        localMediaScanner = new DefaultLocalMediaScanner(attachmentRepository, pathValidator,
            mediaTrackProbeService);
    }

    @Test
    void scan_episodeClassifiesExtensionsMergesVobSubAndKeepsAmbiguity(@TempDir Path tempDir) {
        UUID directoryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        AttachmentEntity directory = directory(directoryId, driverId, tempDir);
        AttachmentEntity episode2 = file(directoryId, driverId, tempDir, "Episode 2.MKV");
        AttachmentEntity episode10 = file(directoryId, driverId, tempDir, "Episode 10.mp4");
        AttachmentEntity subtitle = file(directoryId, driverId, tempDir, "Episode 2.zh.ASS");
        AttachmentEntity vobSub = file(directoryId, driverId, tempDir, "Episode 10.sub");
        AttachmentEntity vobSubIndex = file(directoryId, driverId, tempDir, "Episode 10.idx");
        AttachmentEntity timedText = file(directoryId, driverId, tempDir, "Episode 2.ttml");
        AttachmentEntity externalAudio = file(directoryId, driverId, tempDir, "Episode 2.flac");
        AttachmentEntity unknown = file(directoryId, driverId, tempDir, "class");

        configureTree(directory, List.of(
            episode10, subtitle, vobSubIndex, unknown, episode2, timedText, externalAudio, vobSub));

        LocalScanPreview preview = localMediaScanner.scan(LocalScanPreviewRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.EPISODE).build()).block();

        assertThat(preview.getItems()).extracting(LocalScanItem::getRelativePath)
            .containsExactly("class", "Episode 2.flac", "Episode 2.MKV", "Episode 2.ttml",
                "Episode 2.zh.ASS", "Episode 10.mp4", "Episode 10.sub");
        assertThat(findByName(preview, "class").getPhysicalType())
            .isEqualTo(MediaPhysicalType.UNKNOWN);
        assertThat(findByName(preview, "class").getRole()).isEqualTo(MediaRole.UNKNOWN);
        assertThat(findByName(preview, "Episode 2.flac").getRole())
            .isEqualTo(MediaRole.AUTO_ASSOCIATED);
        assertThat(findByName(preview, "Episode 2.ttml").getRole())
            .isEqualTo(MediaRole.AUTO_ASSOCIATED);
        assertThat(findByName(preview, "Episode 2.ttml").getPhysicalType())
            .isEqualTo(MediaPhysicalType.SUBTITLE);
        assertThat(findByName(preview, "Episode 10.sub").getRole())
            .isEqualTo(MediaRole.AUTO_ASSOCIATED);
        assertThat(preview.getItems())
            .noneMatch(item -> item.getRelativePath().equals("Episode 10.idx"));
    }

    @Test
    void scan_audioKeepsTimedTextAndDoesNotAssociateAmbiguousFiles(@TempDir Path tempDir) {
        UUID directoryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        AttachmentEntity directory = directory(directoryId, driverId, tempDir);
        AttachmentEntity mp3 = file(directoryId, driverId, tempDir, "Track 1.mp3");
        AttachmentEntity flac = file(directoryId, driverId, tempDir, "Track 1.FLAC");
        AttachmentEntity lyrics = file(directoryId, driverId, tempDir, "Track 1.ttml");
        AttachmentEntity subtitle = file(directoryId, driverId, tempDir, "Track 1.ass");

        configureTree(directory, List.of(lyrics, subtitle, flac, mp3));

        LocalScanPreview preview = localMediaScanner.scan(LocalScanPreviewRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.AUDIO).build()).block();

        assertThat(findByName(preview, "Track 1.mp3").getRole()).isEqualTo(MediaRole.PRIMARY);
        assertThat(findByName(preview, "Track 1.FLAC").getRole()).isEqualTo(MediaRole.PRIMARY);
        assertThat(findByName(preview, "Track 1.ttml").getRole())
            .isEqualTo(MediaRole.PENDING_CONFIRMATION);
        assertThat(findByName(preview, "Track 1.ttml").getPhysicalType())
            .isEqualTo(MediaPhysicalType.LYRICS);
        assertThat(findByName(preview, "Track 1.ttml").getCandidatePrimaryAttachmentId()).isNull();
        assertThat(findByName(preview, "Track 1.ass").getRole()).isEqualTo(MediaRole.UNASSOCIATED);
    }

    @Test
    void scan_recursesFilesWithoutReturningDirectoriesOrCrossDirectoryAssociations(
        @TempDir Path tempDir) {
        UUID directoryId = UUID.randomUUID();
        UUID driverId = UUID.randomUUID();
        AttachmentEntity directory = directory(directoryId, driverId, tempDir);
        AttachmentEntity firstDirectory = directory(
            UUID.randomUUID(), driverId, tempDir.resolve("first"));
        firstDirectory.setParentId(directoryId);
        AttachmentEntity secondDirectory = directory(
            UUID.randomUUID(), driverId, tempDir.resolve("second"));
        secondDirectory.setParentId(directoryId);
        AttachmentEntity video = file(
            firstDirectory.getId(), driverId, tempDir.resolve("first"), "Episode 1.mkv");
        AttachmentEntity subtitle = file(
            secondDirectory.getId(), driverId, tempDir.resolve("second"), "Episode 1.srt");

        when(attachmentRepository.findById(directoryId)).thenReturn(Mono.just(directory));
        when(attachmentRepository.findAllByParentId(directoryId))
            .thenReturn(Flux.just(firstDirectory, secondDirectory));
        when(attachmentRepository.findAllByParentId(firstDirectory.getId()))
            .thenReturn(Flux.just(video));
        when(attachmentRepository.findAllByParentId(secondDirectory.getId()))
            .thenReturn(Flux.just(subtitle));
        when(pathValidator.validate(eq(driverId), anyString()))
            .thenAnswer(invocation -> Mono.just(Path.of(invocation.getArgument(1, String.class))));
        when(mediaTrackProbeService.probe(org.mockito.ArgumentMatchers.any(Path.class)))
            .thenReturn(Mono.just(MediaTrackProbeService.ProbeResult.success(List.of())));

        LocalScanPreview preview = localMediaScanner.scan(LocalScanPreviewRequest.builder()
            .directoryId(directoryId).mode(LocalMediaMode.EPISODE).build()).block();

        assertThat(preview.getItems()).extracting(LocalScanItem::getRelativePath)
            .containsExactly("first/Episode 1.mkv", "second/Episode 1.srt");
        assertThat(findByName(preview, "second/Episode 1.srt").getRole())
            .isEqualTo(MediaRole.PENDING_CONFIRMATION);
    }

    private void configureTree(AttachmentEntity directory, List<AttachmentEntity> children) {
        when(attachmentRepository.findById(directory.getId())).thenReturn(Mono.just(directory));
        when(attachmentRepository.findAllByParentId(directory.getId()))
            .thenReturn(Flux.fromIterable(children));
        when(pathValidator.validate(eq(directory.getDriverId()), anyString()))
            .thenAnswer(invocation -> Mono.just(Path.of(invocation.getArgument(1, String.class))));
        when(mediaTrackProbeService.probe(org.mockito.ArgumentMatchers.any(Path.class)))
            .thenReturn(Mono.just(MediaTrackProbeService.ProbeResult.success(List.of())));
    }

    private AttachmentEntity directory(UUID directoryId, UUID driverId, Path path) {
        return AttachmentEntity.builder().id(directoryId).driverId(driverId)
            .fsPath(path.toString()).type(AttachmentType.Driver_Directory).name("root").build();
    }

    private AttachmentEntity file(UUID directoryId, UUID driverId, Path root, String filename) {
        return AttachmentEntity.builder().id(UUID.randomUUID()).parentId(directoryId)
            .driverId(driverId).fsPath(root.resolve(filename).toString())
            .type(AttachmentType.Driver_File).name(filename).build();
    }

    private LocalScanItem findByName(LocalScanPreview preview, String name) {
        return preview.getItems().stream()
            .filter(item -> item.getRelativePath().equals(name)).findFirst()
            .orElseThrow();
    }
}
