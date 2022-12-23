package run.ikaros.server.openapi;

import io.swagger.v3.oas.annotations.tags.Tag;
import run.ikaros.server.result.CommonResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author guohao
 * @date 2022/09/11
 */
@Tag(name = "状态")
@RestController
@RequestMapping("/status")
public class StatusRestController {

    @GetMapping("/ping")
    public CommonResult<String> ping() {
        return CommonResult.ok("pong");
    }

    @GetMapping("/hello")
    public CommonResult<String> hello() {
        return CommonResult.ok("Hello World!");
    }

    @GetMapping("/running")
    public CommonResult<String> running() {
        return CommonResult.ok("RUNNING");
    }

}
