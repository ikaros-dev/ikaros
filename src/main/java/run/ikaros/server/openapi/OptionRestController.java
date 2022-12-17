package run.ikaros.server.openapi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.core.service.OptionService;
import run.ikaros.server.enums.OptionCategory;
import run.ikaros.server.model.dto.OptionDTO;
import run.ikaros.server.model.request.AppInitRequest;
import run.ikaros.server.model.request.SaveOptionRequest;
import run.ikaros.server.model.response.OptionResponse;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.utils.StringUtils;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author guohao
 * @date 2022/10/19
 */
@RestController
@RequestMapping("/option")
public class OptionRestController {
    private static final Logger LOGGER = LoggerFactory.getLogger(OptionRestController.class);
    private final OptionService optionService;

    public OptionRestController(OptionService optionService) {
        this.optionService = optionService;
    }

    @GetMapping("/app/is-init")
    public CommonResult<Boolean> findAppIsInit() {
        return CommonResult.ok(optionService.findAppIsInit());
    }

    @PostMapping("/app/init")
    public CommonResult<Boolean> reqAppInit(@RequestBody AppInitRequest appInitRequest) {
        return CommonResult.ok(optionService.appInit(appInitRequest));
    }

    @PostMapping("/app/init/coerce")
    public CommonResult<Boolean> reqAppCoerceInit(@RequestBody AppInitRequest appInitRequest) {
        return CommonResult.ok(optionService.appInit(appInitRequest, true));
    }

    @GetMapping("/list")
    public CommonResult<List<OptionResponse>> findOptionList(
        @RequestParam(name = "category", required = false) String categoryQueryParam) {
        Map<String, OptionResponse> categoryOptionResponseMap = new HashMap<>();
        List<OptionDTO> optionDTOList = optionService.findOptions(categoryQueryParam);
        for (OptionDTO optionDTO : optionDTOList) {
            String category = optionDTO.getCategory();
            if (categoryOptionResponseMap.containsKey(category)) {
                OptionResponse optionResponse = categoryOptionResponseMap.get(category);
                Map<String, String> kvMap = optionResponse.getKvMap();
                kvMap.put(optionDTO.getKey(), optionDTO.getValue());
            } else {
                Map<String, String> kvMap = new HashMap<>();
                kvMap.put(optionDTO.getKey(), optionDTO.getValue());
                OptionResponse optionResponse = new OptionResponse();
                optionResponse.setKvMap(kvMap);
                optionResponse.setCategory(category);
                categoryOptionResponseMap.put(category, optionResponse);
            }
        }
        return CommonResult.ok(categoryOptionResponseMap.values().stream().toList());
    }

    @PostMapping
    public CommonResult<OptionResponse> saveOptionWithCategory(
        @RequestBody @Valid SaveOptionRequest saveOptionRequest
    ) {
        OptionResponse optionResponse = new OptionResponse();
        List<OptionDTO> optionDTOList = optionService.saveWithRequest(saveOptionRequest);
        Map<String, String> kvMap = new HashMap<>();
        for (OptionDTO optionDTO : optionDTOList) {
            String category = optionDTO.getCategory();
            optionResponse.setCategory(category);
            kvMap.put(optionDTO.getKey(), optionDTO.getValue());
        }
        optionResponse.setKvMap(kvMap);
        return CommonResult.ok(optionResponse);
    }

}
