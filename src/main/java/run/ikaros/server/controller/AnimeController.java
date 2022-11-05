package run.ikaros.server.controller;

import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import run.ikaros.server.constants.AppConst;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.service.AnimeService;
import run.ikaros.server.service.ThemeService;

/**
 * @author li-guohao
 */
@Controller
@RequestMapping("/anime")
public class AnimeController {

    private final ThemeService themeService;
    private final AnimeService animeService;

    public AnimeController(ThemeService themeService, AnimeService animeService) {
        this.themeService = themeService;
        this.animeService = animeService;
    }

    @RequestMapping
    public String index(Model model) {
        List<AnimeEntity> animeList = animeService.listAll();
        model.addAttribute("animeList", animeList);
        return themeService.getComplexPagePostfix()  + "anime";
    }

    @RequestMapping("/dto/id/{id}")
    public String anime(@PathVariable Long id, Model model) {
        model.addAttribute("anime", animeService.findAnimeDTOById(id));
        return themeService.getComplexPagePostfix()  + "anime-info";
    }

}
