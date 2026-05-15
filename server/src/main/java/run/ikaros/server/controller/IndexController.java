package run.ikaros.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.server.theme.ThemeService;

@Controller
public class IndexController {
    private final ThemeService themeService;
    private final IkarosProperties ikarosProperties;

    public IndexController(ThemeService themeService, IkarosProperties ikarosProperties) {
        this.themeService = themeService;
        this.ikarosProperties = ikarosProperties;
    }

    /**
     * Default index path.
     */
    @RequestMapping("/")
    public Mono<String> index(Model model) {
        if (ikarosProperties.getShowTheme()) {
            return themeService.getCurrentTheme()
                .map(theme -> theme + "/index");
        } else {
            return Mono.just("default/index");
        }
    }
}
