package run.ikaros.identity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping({"/api/permissions", "/api/admin/permissions"})
public class PermissionController {
    @GetMapping
    public Flux<String> list() {
        return Flux.fromIterable(PlatformPermission.registeredKeys());
    }
}
