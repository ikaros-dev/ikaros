package run.ikaros.server.controller;

import java.io.File;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

/**
 * @author li-guohao
 */
@Controller
public class ThemeController {

    private static final String DEFAULT_THEME = "simple";

    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("msg", "Hello Ikaros");
        return "themes" + File.separator + DEFAULT_THEME + File.separator + "index";
    }

}
