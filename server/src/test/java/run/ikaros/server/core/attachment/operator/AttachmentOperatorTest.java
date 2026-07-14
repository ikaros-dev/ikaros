package run.ikaros.server.core.attachment.operator;

import static org.mockito.Mockito.when;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import run.ikaros.api.core.attachment.Attachment;
import run.ikaros.api.core.attachment.AttachmentReference;
import run.ikaros.api.core.attachment.AttachmentRelation;
import run.ikaros.api.core.attachment.AttachmentSearchCondition;
import run.ikaros.api.core.attachment.AttachmentUploadCondition;
import run.ikaros.api.core.attachment.AttachmentDriver;
import run.ikaros.api.store.enums.AttachmentReferenceType;
import run.ikaros.api.store.enums.AttachmentRelationType;
import run.ikaros.api.store.enums.AttachmentDriverType;
import run.ikaros.api.store.enums.AttachmentType;
import run.ikaros.api.wrap.PagingWrap;
import run.ikaros.server.core.attachment.service.AttachmentDriverService;
import run.ikaros.server.core.attachment.service.AttachmentReferenceService;
import run.ikaros.server.core.attachment.service.AttachmentRelationService;
import run.ikaros.server.core.attachment.service.AttachmentService;

@ExtendWith(MockitoExtension.class)
class AttachmentOperatorTest {

    @Mock
    private AttachmentService attachmentService;
    @Mock
    private AttachmentReferenceService referenceService;
    @Mock
    private AttachmentRelationService relationService;
    @Mock
    private AttachmentDriverService driverService;

    private AttachmentOperator attachmentOperator;
    private AttachmentReferenceOperator referenceOperator;
    private AttachmentRelationOperator relationOperator;
    private AttachmentDriverOperator driverOperator;

    @BeforeEach
    void setUp() {
        attachmentOperator = new AttachmentOperator(attachmentService);
        referenceOperator = new AttachmentReferenceOperator(referenceService);
        relationOperator = new AttachmentRelationOperator(relationService);
        driverOperator = new AttachmentDriverOperator(driverService);
    }

    @Test
    void attachmentSave() {
        Attachment attachment = Attachment.builder().name("test.txt").build();
        when(attachmentService.save(attachment)).thenReturn(Mono.just(attachment));
        StepVerifier.create(attachmentOperator.save(attachment))
            .expectNext(attachment)
            .verifyComplete();
    }

    @Test
    void attachmentListByCondition() {
        AttachmentSearchCondition condition = AttachmentSearchCondition.builder().build();
        PagingWrap<Attachment> wrap = PagingWrap.emptyResult();
        when(attachmentService.listByCondition(condition)).thenReturn(Mono.just(wrap));
        StepVerifier.create(attachmentOperator.listByCondition(condition))
            .expectNext(wrap)
            .verifyComplete();
    }

    @Test
    void attachmentUpload() {
        AttachmentUploadCondition uploadCondition = AttachmentUploadCondition.builder().build();
        Attachment attachment = Attachment.builder().name("uploaded.png").build();
        when(attachmentService.upload(uploadCondition)).thenReturn(Mono.just(attachment));
        StepVerifier.create(attachmentOperator.upload(uploadCondition))
            .expectNext(attachment)
            .verifyComplete();
    }

    @Test
    void attachmentFindById() {
        UUID id = UUID.randomUUID();
        Attachment attachment = Attachment.builder().id(id).name("test.txt").build();
        when(attachmentService.findById(id)).thenReturn(Mono.just(attachment));
        StepVerifier.create(attachmentOperator.findById(id))
            .expectNext(attachment)
            .verifyComplete();
    }

    @Test
    void attachmentFindByTypeAndParentIdAndName() {
        UUID parentId = UUID.randomUUID();
        Attachment attachment = Attachment.builder().name("child.txt").build();
        when(attachmentService.findByTypeAndParentIdAndName(AttachmentType.File, parentId, "child.txt"))
            .thenReturn(Mono.just(attachment));
        StepVerifier.create(
                attachmentOperator.findByTypeAndParentIdAndName(AttachmentType.File, parentId, "child.txt"))
            .expectNext(attachment)
            .verifyComplete();
    }

    @Test
    void attachmentCreateDirectory() {
        UUID parentId = UUID.randomUUID();
        Attachment dir = Attachment.builder().name("newdir").type(AttachmentType.Directory).build();
        when(attachmentService.createDirectory(parentId, "newdir")).thenReturn(Mono.just(dir));
        StepVerifier.create(attachmentOperator.createDirectory(parentId, "newdir"))
            .expectNext(dir)
            .verifyComplete();
    }

    @Test
    void attachmentExistsByParentIdAndName() {
        UUID parentId = UUID.randomUUID();
        when(attachmentService.existsByParentIdAndName(parentId, "test.txt")).thenReturn(Mono.just(true));
        StepVerifier.create(attachmentOperator.existsByParentIdAndName(parentId, "test.txt"))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void attachmentExistsByTypeAndParentIdAndName() {
        UUID parentId = UUID.randomUUID();
        when(attachmentService.existsByTypeAndParentIdAndName(AttachmentType.File, parentId, "test.txt"))
            .thenReturn(Mono.just(false));
        StepVerifier.create(
                attachmentOperator.existsByTypeAndParentIdAndName(AttachmentType.File, parentId, "test.txt"))
            .expectNext(false)
            .verifyComplete();
    }

    @Test
    void referenceSave() {
        AttachmentReference ref = AttachmentReference.builder().build();
        when(referenceService.save(ref)).thenReturn(Mono.just(ref));
        StepVerifier.create(referenceOperator.save(ref))
            .expectNext(ref)
            .verifyComplete();
    }

    @Test
    void referenceFindAllByTypeAndAttachmentId() {
        UUID attachmentId = UUID.randomUUID();
        AttachmentReference ref = AttachmentReference.builder().attachmentId(attachmentId).build();
        when(referenceService.findAllByTypeAndAttachmentId(AttachmentReferenceType.EPISODE, attachmentId))
            .thenReturn(Flux.just(ref));
        StepVerifier.create(
                referenceOperator.findAllByTypeAndAttachmentId(AttachmentReferenceType.EPISODE, attachmentId))
            .expectNext(ref)
            .verifyComplete();
    }

    @Test
    void referenceMatchingAttachmentsAndSubjectEpisodes() {
        UUID subjectId = UUID.randomUUID();
        UUID[] attachmentIds = {UUID.randomUUID()};
        when(referenceService.matchingAttachmentsAndSubjectEpisodes(subjectId, attachmentIds))
            .thenReturn(Mono.empty());
        StepVerifier.create(referenceOperator.matchingAttachmentsAndSubjectEpisodes(subjectId, attachmentIds))
            .verifyComplete();
    }

    @Test
    void referenceMatchingAttachmentsAndSubjectEpisodesWithNotify() {
        UUID subjectId = UUID.randomUUID();
        UUID[] attachmentIds = {UUID.randomUUID()};
        when(referenceService.matchingAttachmentsAndSubjectEpisodes(subjectId, attachmentIds, true))
            .thenReturn(Mono.empty());
        StepVerifier.create(
                referenceOperator.matchingAttachmentsAndSubjectEpisodes(subjectId, attachmentIds, true))
            .verifyComplete();
    }

    @Test
    void referenceMatchingAttachmentsForEpisode() {
        UUID episodeId = UUID.randomUUID();
        UUID[] attachmentIds = {UUID.randomUUID()};
        when(referenceService.matchingAttachmentsForEpisode(episodeId, attachmentIds))
            .thenReturn(Mono.empty());
        StepVerifier.create(referenceOperator.matchingAttachmentsForEpisode(episodeId, attachmentIds))
            .verifyComplete();
    }

    @Test
    void relationFindAllByTypeAndAttachmentId() {
        UUID attachmentId = UUID.randomUUID();
        AttachmentRelation relation = AttachmentRelation.builder().attachmentId(attachmentId).build();
        when(relationService.findAllByTypeAndAttachmentId(AttachmentRelationType.VIDEO_SUBTITLE, attachmentId))
            .thenReturn(Flux.just(relation));
        StepVerifier.create(
                relationOperator.findAllByTypeAndAttachmentId(AttachmentRelationType.VIDEO_SUBTITLE, attachmentId))
            .expectNext(relation)
            .verifyComplete();
    }

    @Test
    void driverSave() {
        AttachmentDriver driver = AttachmentDriver.builder().name("local").build();
        when(driverService.save(driver)).thenReturn(Mono.just(driver));
        StepVerifier.create(driverOperator.save(driver))
            .expectNext(driver)
            .verifyComplete();
    }

    @Test
    void driverFindById() {
        UUID id = UUID.randomUUID();
        AttachmentDriver driver = AttachmentDriver.builder().id(id).build();
        when(driverService.findById(id)).thenReturn(Mono.just(driver));
        StepVerifier.create(driverOperator.findById(id))
            .expectNext(driver)
            .verifyComplete();
    }

    @Test
    void driverFindByTypeAndName() {
        AttachmentDriver driver = AttachmentDriver.builder().name("local").type(AttachmentDriverType.LOCAL).build();
        when(driverService.findByTypeAndName("LOCAL", "local")).thenReturn(Mono.just(driver));
        StepVerifier.create(driverOperator.findByTypeAndName("LOCAL", "local"))
            .expectNext(driver)
            .verifyComplete();
    }

    @Test
    void driverListAttachmentsByCondition() {
        AttachmentSearchCondition condition = AttachmentSearchCondition.builder().build();
        PagingWrap<Attachment> wrap = PagingWrap.emptyResult();
        when(driverService.listAttachmentsByCondition(condition)).thenReturn(Mono.just(wrap));
        StepVerifier.create(driverOperator.listAttachmentsByCondition(condition))
            .expectNext(wrap)
            .verifyComplete();
    }

    @Test
    void driverRefresh() {
        UUID id = UUID.randomUUID();
        when(driverService.refresh(id)).thenReturn(Mono.empty());
        StepVerifier.create(driverOperator.refresh(id)).verifyComplete();
    }

    @Test
    void driverListDriversByCondition() {
        PagingWrap<AttachmentDriver> wrap = PagingWrap.emptyResult();
        when(driverService.listDriversByCondition(1, 10)).thenReturn(Mono.just(wrap));
        StepVerifier.create(driverOperator.listDriversByCondition(1, 10))
            .expectNext(wrap)
            .verifyComplete();
    }
}
