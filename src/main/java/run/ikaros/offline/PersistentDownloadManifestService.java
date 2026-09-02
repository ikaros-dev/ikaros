package run.ikaros.offline;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import run.ikaros.common.NotFoundException;
@Service
public class PersistentDownloadManifestService implements DownloadManifestService {
    private final DownloadIntentRepository intents;
    private final DownloadManifestRepository manifests;
    private final DownloadManifestItemRepository items;
    public PersistentDownloadManifestService(DownloadIntentRepository intents, DownloadManifestRepository manifests, DownloadManifestItemRepository items){this.intents=intents;this.manifests=manifests;this.items=items;}
    @Override public Mono<DownloadManifestView> create(UUID user,UUID intentId,CreateDownloadManifestRequest req){return owned(user,intentId).flatMap(intent->manifests.findTopByIntentIdOrderByManifestVersionDesc(intentId).defaultIfEmpty(new DownloadManifestEntity(null,intentId,0,null)).flatMap(previous->{long version=previous.manifestVersion()+1;Instant now=Instant.now();DownloadManifestEntity manifest=new DownloadManifestEntity(null,intentId,version,now);return manifests.save(manifest).flatMap(saved->reactor.core.publisher.Flux.fromIterable(req.items()).map(i->new DownloadManifestItemEntity(null,saved.id(),i.attachmentId(),i.sizeBytes(),i.sha256(),i.required())).flatMap(items::save).then(getByManifest(saved)));}));}
    @Override public Mono<DownloadManifestView> get(UUID user,UUID intentId){return owned(user,intentId).then(manifests.findTopByIntentIdOrderByManifestVersionDesc(intentId).switchIfEmpty(Mono.error(new NotFoundException("Download Manifest 不存在"))).flatMap(this::getByManifest));}
    private Mono<DownloadIntentEntity> owned(UUID user,UUID id){return intents.findById(id).filter(i->i.userId().equals(user)).switchIfEmpty(Mono.error(new NotFoundException("Download 不存在")));}
    private Mono<DownloadManifestView> getByManifest(DownloadManifestEntity m){return items.findAllByManifestId(m.id()).map(i->new ManifestItemView(i.id(),i.attachmentId(),i.sizeBytes(),i.sha256(),i.required())).collectList().map(list->new DownloadManifestView(m.id(),m.intentId(),m.manifestVersion(),list,m.generatedAt()));}
}
