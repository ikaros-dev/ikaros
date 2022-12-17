package run.ikaros.server.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.core.service.ThemeService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author li-guohao
 */
@Controller
public class ThemeController {

    private final ThemeService themeService;
    private final OptionService optionService;

    public ThemeController(ThemeService themeService, OptionService optionService) {
        this.themeService = themeService;
        this.optionService = optionService;
    }

    @RequestMapping("/")
    public String index(Model model, HttpServletRequest req, HttpServletResponse resp)
        throws IOException {
        if (!optionService.findAppIsInit()) {
            resp.sendRedirect(req.getContextPath() + "/admin/index.html#/app/init");
        }
        model.addAttribute("msg", "Hello Ikaros");
        return themeService.getComplexPagePostfix() + "index";
    }
}
