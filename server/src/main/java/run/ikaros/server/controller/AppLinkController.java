package run.ikaros.server.controller;

import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.infra.utils.StringUtils;
import run.ikaros.server.core.subject.service.SubjectService;

@Controller
@RequestMapping("/app/link")
public class AppLinkController {
    private final SubjectService subjectService;
    private final IkarosProperties ikarosProperties;

    public AppLinkController(SubjectService subjectService, IkarosProperties ikarosProperties) {
        this.subjectService = subjectService;
        this.ikarosProperties = ikarosProperties;
    }

    /**
     * app 链接跳转页面.
     */
    @GetMapping("/subject/{id}")
    public Mono<String> index(Model model, @PathVariable("id") UUID subjectId) {
        return subjectService.findById(subjectId)
            .flatMap(subject -> {
                String name = StringUtils.isBlank(subject.getNameCn())
                    ? subject.getName() : subject.getNameCn();
                model.addAttribute("subject", subject);
                model.addAttribute("subjectName", name);
                String cover = subject.getCover();
                if (StringUtils.isNotBlank(cover) && !cover.startsWith("http")) {
                    cover = ikarosProperties.getExternalUrl() + cover;
                }
                subject.setCover(cover);
                model.addAttribute("subjectCover", cover);
                String subjectUrl = "ikaros://app/subject/" + subject.getId();
                model.addAttribute("subjectUrl", subjectUrl);
                return Mono.just("temp/app-link-to");
            });
    }
}
