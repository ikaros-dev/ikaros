package run.ikaros.ingestion;
import jakarta.validation.Valid; import java.util.List; import java.util.UUID; import org.springframework.web.bind.annotation.*; import reactor.core.publisher.Mono;
@RestController @RequestMapping({"/api/ingestion/resources"})
public class MetadataCandidateController { private final MetadataCandidateService service; public MetadataCandidateController(MetadataCandidateService service){this.service=service;}
 @PostMapping("/{resourceId}/metadata-candidates") public Mono<MetadataCandidateView> submit(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID resourceId,@Valid @RequestBody SubmitMetadataCandidateRequest request){return service.submit(actor,resourceId,request);}
 @GetMapping("/{resourceId}/metadata-candidates") public Mono<List<MetadataCandidateView>> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID resourceId){return service.list(actor,resourceId);}}
