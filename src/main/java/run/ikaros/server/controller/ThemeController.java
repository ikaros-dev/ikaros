package run.ikaros.server.controller;

import static run.ikaros.server.constants.AppConst.DEFAULT_THEME;

import java.io.File;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import run.ikaros.server.constants.AppConst;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.service.AnimeService;

/**
 * @author li-guohao
 */
@Controller
public class ThemeController {

    @RequestMapping("/")
    public String index(Model model) {
        model.addAttribute("msg", "Hello Ikaros");
        return AppConst.PAGE_POSTFIX + "index";
    }
}
