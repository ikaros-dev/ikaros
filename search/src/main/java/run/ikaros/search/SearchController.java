package run.ikaros.search;

import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final SearchQueryService service;

    public SearchController(SearchQueryService service) { this.service = service; }

    @GetMapping
    public Mono<SearchPage> search(@RequestHeader("X-Ikaros-Actor-Id") UUID actorId,
                                   @RequestParam String q,
                                   @RequestParam(required = false) String cursor,
                                   @RequestParam(required = false) Integer limit) {
        return service.search(actorId, new SearchQueryRequest(q, cursor, limit));
    }
}
