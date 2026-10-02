package run.ikaros.storage;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import run.ikaros.storage.api.AdminAttachmentPage;

/** Administrative read-only listing of active attachments across users. */
@Validated
@RestController
@RequestMapping("/api/admin/attachments")
public class AdminAttachmentController {
    private final AdminAttachmentQueryService queryService;

    public AdminAttachmentController(AdminAttachmentQueryService queryService) {
        this.queryService = queryService;
    }

    @GetMapping
    public Mono<AdminAttachmentPage> list(
        @RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
        @RequestParam(required = false) @Size(max = 256) String query
    ) {
        return queryService.listAll(actorId, page, size, query);
    }
}
