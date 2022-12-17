package run.ikaros.server.openapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import run.ikaros.server.core.service.NotifyService;
import run.ikaros.server.model.request.NotifyMailTestRequest;
import run.ikaros.server.result.CommonResult;

import jakarta.mail.MessagingException;

@RestController
@RequestMapping("/notify")
public class NotifyRestController {

    private final NotifyService notifyService;

    public NotifyRestController(NotifyService notifyService) {
        this.notifyService = notifyService;
    }


    @PostMapping("/mail/test")
    public CommonResult<Boolean> mailTest(@RequestBody NotifyMailTestRequest notifyMailTestRequest)
        throws MessagingException {
        notifyService.mailTest(notifyMailTestRequest);
        return CommonResult.ok(Boolean.TRUE);
    }

}
