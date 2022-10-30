package run.ikaros.server.controller;

import static run.ikaros.server.constants.AppConst.DEFAULT_THEME;

import java.io.File;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import run.ikaros.server.constants.AppConst;
import run.ikaros.server.entity.AnimeEntity;
import run.ikaros.server.service.AnimeService;

/**
 * @author li-guohao
 */
@Controller
@RequestMapping("/anime")
public class AnimeController {

    private final AnimeService animeService;

    public AnimeController(AnimeService animeService) {
        this.animeService = animeService;
    }

    @RequestMapping
    public String index(Model model) {
        List<AnimeEntity> animeList = animeService.listAll();
        model.addAttribute("animeList", animeList);
        return AppConst.PAGE_POSTFIX  + "anime";
    }

    @RequestMapping("/dto/id/{id}")
    public String anime(@PathVariable Long id, Model model) {
        model.addAttribute("anime", animeService.findAnimeDTOById(id));
        return AppConst.PAGE_POSTFIX  + "anime-info";
    }

}
