package run.ikaros.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;
import run.ikaros.server.theme.ThemeService;

@Controller
public class IndexController {
    private final ThemeService themeService;

    public IndexController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @RequestMapping("/")
    public Mono<String> index(Model model) {
        return themeService.getCurrentTheme()
            .map(theme -> "/theme/" + theme + "/index");
    }
}
