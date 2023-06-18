package run.ikaros.server.core.subject.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.file.File;
import run.ikaros.api.core.file.FileConst;
import run.ikaros.api.core.file.FilePolicy;
import run.ikaros.api.core.subject.SubjectImage;
import run.ikaros.api.store.entity.FileEntity;
import run.ikaros.server.core.file.FileService;
import run.ikaros.server.core.subject.event.SubjectImageDownloadAndUpdateEvent;
import run.ikaros.server.infra.utils.ReactiveBeanUtils;
import run.ikaros.server.store.entity.SubjectImageEntity;
import run.ikaros.server.store.repository.SubjectImageRepository;

@Slf4j
@Component
public class SubjectImageDownloadListener implements InitializingBean {

    final FilePolicy localFilePolicy  = FilePolicy.builder().name(FileConst.POLICY_LOCAL).build();

    // 当前 User-Agent格式 ikaros-dev/ikaros (https://ikaros.run)
    final String userAgent = "https://ikaros.run (ikaros-dev/ikaros)";
    private WebClient webClient;
    private final FileService fileService;
    private final SubjectImageRepository subjectImageRepository;

    public SubjectImageDownloadListener(FileService fileService,
                                        SubjectImageRepository subjectImageRepository) {
        this.fileService = fileService;
        this.subjectImageRepository = subjectImageRepository;
    }

    /**
     * Handle image download and update subject image url.
     */
    // 目前这块还有问题，没法对接文件上传服务
    // @EventListener(SubjectImageDownloadAndUpdateEvent.class)
    public Mono<Void> handle(SubjectImageDownloadAndUpdateEvent event) {
        Long subjectId = event.getSubjectId();
        SubjectImage image = event.getImage();
        Assert.notNull(image, "'image' must not null.");
        Assert.notNull(subjectId, "'subjectId' must not null.");
        return Mono.just(image)
            .flatMap(subjectImage -> ReactiveBeanUtils.copyProperties(subjectImage,
                new SubjectImageEntity()))
            .map(imageEntity -> imageEntity.setSubjectId(subjectId))
            .flatMap(this::updateCommon)
            .flatMap(this::updateLarge)
            .flatMap(this::updateMedium)
            .flatMap(this::updateSmall)
            .flatMap(this::updateGrid)
            .doOnSuccess(imageEntity ->
                log.debug("Update subject image entity for subject id={}.", subjectId))
            .then();
    }

    private Mono<String> downloadImage(String url) {
        return webClient.get()
            .exchangeToMono(clientResponse -> clientResponse.bodyToMono(FilePart.class))
            .flatMap(filePart -> fileService.upload(localFilePolicy, filePart)
                .map(File::entity)
                .map(FileEntity::getUrl));

    }

    private Mono<SubjectImageEntity> updateCommon(SubjectImageEntity imageEntity) {
        return downloadImage(imageEntity.getCommon())
            .map(imageEntity::setCommon)
            .flatMap(subjectImageRepository::save);
    }

    private Mono<SubjectImageEntity> updateLarge(SubjectImageEntity imageEntity) {
        return downloadImage(imageEntity.getLarge())
            .map(imageEntity::setLarge)
            .flatMap(subjectImageRepository::save);
    }

    private Mono<SubjectImageEntity>  updateMedium(SubjectImageEntity imageEntity) {
        return downloadImage(imageEntity.getMedium())
            .map(imageEntity::setMedium)
            .flatMap(subjectImageRepository::save);
    }

    private Mono<SubjectImageEntity>  updateSmall(SubjectImageEntity imageEntity) {
        return downloadImage(imageEntity.getSmall())
            .map(imageEntity::setSmall)
            .flatMap(subjectImageRepository::save);
    }

    private Mono<SubjectImageEntity>  updateGrid(SubjectImageEntity imageEntity) {
        return downloadImage(imageEntity.getGrid())
            .map(imageEntity::setGrid)
            .flatMap(subjectImageRepository::save);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
            .build();
    }
}
