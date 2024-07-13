package run.ikaros.server.controller;

import java.util.ArrayList;
import java.util.Objects;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.api.core.attachment.VideoSubtitle;
import run.ikaros.api.core.subject.Subject;
import run.ikaros.api.core.subject.vo.FindSubjectCondition;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.core.attachment.service.AttachmentRelationService;
import run.ikaros.server.core.statics.StaticService;
import run.ikaros.server.core.subject.service.SubjectService;
import run.ikaros.server.theme.ThemeService;

@Controller
@RequestMapping("/subject")
public class SubjectController {

    private final SubjectService subjectService;
    private final ThemeService themeService;
    private final AttachmentRelationService attachmentRelationService;
    private final StaticService staticService;

    /**
     * Construct.
     */
    public SubjectController(SubjectService subjectService, ThemeService themeService,
                             AttachmentRelationService attachmentRelationService,
                             StaticService staticService) {
        this.subjectService = subjectService;
        this.themeService = themeService;
        this.attachmentRelationService = attachmentRelationService;
        this.staticService = staticService;
    }


    /**
     * Subject list page.
     */
    @GetMapping("/list")
    public Mono<String> index(Model model, Integer page, Integer size, SubjectType type) {
        if (Objects.isNull(page) || page <= 0) {
            page = 1;
        }
        if (Objects.isNull(size) || size <= 0) {
            size = 8;
        }
        return subjectService.listEntitiesByCondition(FindSubjectCondition.builder()
                .page(page).size(size).type(type).nsfw(false)
                .build())
            .map(pagingWarp -> model.addAttribute("pagingWarp", pagingWarp))
            .then(themeService.getCurrentTheme())
            .map(theme -> theme + "/" + "subjects");
    }

    /**
     * Get subject details by id.
     */
    @GetMapping("/{id}")
    public Mono<String> findById(@PathVariable("id") Long id,
                                 @RequestParam("episode") Integer epSeq, Model model) {
        return subjectService.findById(id)
            .flatMap(subject -> addEpisodesResourcesAttachmentIdSubtitleMap2Model(subject, model))
            .map(subject -> model.addAttribute("subject", subject))
            .flatMap(m1 -> staticService.listStaticsFonts().collectList()
                .map(fonts -> m1.addAttribute("fonts", fonts)))
            .then(themeService.getCurrentTheme())
            .map(theme -> theme + "/" + "subject-details");
    }

    private Mono<Subject> addEpisodesResourcesAttachmentIdSubtitleMap2Model(Subject subject,
                                                                            Model model) {
        return Mono.just(new ArrayList<VideoSubtitle>())
            .flatMap(videoSubtitleList -> Flux.fromStream(subject.getEpisodes().stream())
                .filter(episode -> Objects.nonNull(episode.getResources())
                    && !episode.getResources().isEmpty())
                .map(episode -> episode.getResources().get(0).getAttachmentId())
                .flatMap(attachmentId ->
                    attachmentRelationService.findAttachmentVideoSubtitles(attachmentId)
                        .collectList()
                        .map(videoSubtitles -> {
                            videoSubtitleList.addAll(videoSubtitles);
                            return videoSubtitles;
                        }))
                .then(Mono.just(videoSubtitleList)))
            .map(videoSubtitleList -> model.addAttribute("videoSubtitleList", videoSubtitleList))
            .then(Mono.just(subject));
    }
}
