package run.ikaros.server.openapi;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.entity.OptionEntity;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.service.impl.OptionServiceImpl;

/**
 * @author guohao
 * @date 2022/10/19
 */
@RestController
@RequestMapping("/option")
public class OptionRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptionRestController.class);
    private final OptionServiceImpl optionServiceImpl;

    public OptionRestController(OptionServiceImpl optionServiceImpl) {
        this.optionServiceImpl = optionServiceImpl;
    }


    @GetMapping("/category")
    public CommonResult<List<OptionEntity>> findOptionListCategory(@RequestParam String category) {
        // todo impl
        return null;
    }


}
