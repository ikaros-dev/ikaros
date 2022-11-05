package run.ikaros.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import run.ikaros.server.constants.AppConst;
import run.ikaros.server.service.ThemeService;

/**
 * @author li-guohao
 */
@Controller
public class ThemeController {

    private final ThemeService themeService;

    public ThemeController(ThemeService themeService) {
        this.themeService = themeService;
    }

    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("msg", "Hello Ikaros");
        return themeService.getComplexPagePostfix() + "index";
    }
}
