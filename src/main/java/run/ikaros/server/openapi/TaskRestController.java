package run.ikaros.server.openapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.result.CommonResult;
import run.ikaros.server.core.service.TaskService;

@RestController
@RequestMapping("/task")
public class TaskRestController {
    private final TaskService taskService;

    public TaskRestController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents")
    public CommonResult<String> pullAnimeSubscribeAndSaveMetadataAndDownloadTorrents() {
        taskService.pullMikanRssAnimeSubscribeAndSaveMetadataAndDownloadTorrents();
        return CommonResult.ok();
    }

    @GetMapping("/searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode")
    public CommonResult<String> searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode() {
        taskService.searchDownloadProcessAndCreateFileHardLinksAndRelateEpisode();
        return CommonResult.ok();
    }

}
