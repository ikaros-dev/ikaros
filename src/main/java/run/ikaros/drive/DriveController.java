package run.ikaros.drive;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import run.ikaros.common.IfMatchVersion;
@RestController
@RequestMapping("/api/drive")
public class DriveController {
    private final DriveService service;
    public DriveController(DriveService service) { this.service = service; }
    @PostMapping("/spaces") public Mono<DriveSpaceView> create(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@Valid @RequestBody CreateDriveSpaceRequest request){return service.createSpace(actor,request);}
    @GetMapping("/spaces") public Flux<DriveSpaceView> list(@RequestHeader("X-Ikaros-Actor-Id") UUID actor){return service.listSpaces(actor);}
    @GetMapping("/spaces/{spaceId}/children") public Flux<DriveNodeView> children(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID spaceId,@RequestParam(required=false) UUID parentId){return service.children(actor,spaceId,parentId);}
    @PostMapping("/spaces/{spaceId}/nodes") public Mono<DriveNodeView> createNode(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID spaceId,@Valid @RequestBody CreateDriveNodeRequest request){return service.createNode(actor,spaceId,request);}
    @PatchMapping("/nodes/{nodeId}") public Mono<ResponseEntity<DriveNodeView>> rename(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID nodeId,@RequestHeader(value="If-Match",required=false) String ifMatch,@Valid @RequestBody RenameDriveNodeRequest request){long version=IfMatchVersion.parse(ifMatch);return service.rename(actor,nodeId,new RenameDriveNodeRequest(request.name(),version)).map(view->withEtag(view));}
    @PostMapping("/nodes/{nodeId}/move") public Mono<ResponseEntity<DriveNodeView>> move(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID nodeId,@RequestHeader(value="If-Match",required=false) String ifMatch,@Valid @RequestBody MoveDriveNodeRequest request){long version=IfMatchVersion.parse(ifMatch);return service.move(actor,nodeId,new MoveDriveNodeRequest(request.parentId(),version)).map(view->withEtag(view));}
    @PostMapping("/nodes/{nodeId}/trash") public Mono<ResponseEntity<DriveNodeView>> trash(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID nodeId,@RequestHeader(value="If-Match",required=false) String ifMatch){return service.trash(actor,nodeId,IfMatchVersion.parse(ifMatch)).map(view->withEtag(view));}
    @PostMapping("/nodes/{nodeId}/restore") public Mono<ResponseEntity<DriveNodeView>> restore(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID nodeId,@RequestHeader(value="If-Match",required=false) String ifMatch){return service.restore(actor,nodeId,IfMatchVersion.parse(ifMatch)).map(view->withEtag(view));}
    @PostMapping("/nodes/{nodeId}/revisions") public Mono<DriveRevisionView> revision(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID nodeId,@Valid @RequestBody CreateDriveRevisionRequest request){return service.createRevision(actor,nodeId,request);}
    @GetMapping("/nodes/{nodeId}/revisions") public Flux<DriveRevisionView> revisions(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID nodeId){return service.revisions(actor,nodeId);}
    @GetMapping("/spaces/{spaceId}/changes") public Flux<DriveChangeView> changes(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID spaceId,@RequestParam(defaultValue="0") long afterSequence){return service.changes(actor,spaceId,afterSequence);}
    @GetMapping("/spaces/{spaceId}/quota") public Mono<DriveQuotaView> quota(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID spaceId){return service.quota(actor,spaceId);}
    @PostMapping("/spaces/{spaceId}/uploads") public Mono<DriveQuotaReservationView> beginUpload(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID spaceId,@Valid @RequestBody BeginDriveUploadRequest request){return service.beginUpload(actor,spaceId,request);}
    @PostMapping("/spaces/{spaceId}/uploads/{reservationId}/finalize") public Mono<DriveQuotaReservationView> finalizeUpload(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID spaceId,@PathVariable UUID reservationId){return service.finalizeUpload(actor,spaceId,reservationId);}
    @PostMapping("/spaces/{spaceId}/uploads/{reservationId}/abort") public Mono<DriveQuotaReservationView> abortUpload(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID spaceId,@PathVariable UUID reservationId){return service.abortUpload(actor,spaceId,reservationId);}
    @PostMapping("/bindings") public Mono<SyncBindingView> createBinding(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@Valid @RequestBody CreateSyncBindingRequest request){return service.createBinding(actor,request);}
    @GetMapping("/bindings") public Flux<SyncBindingView> bindings(@RequestHeader("X-Ikaros-Actor-Id") UUID actor){return service.bindings(actor);}
    @PostMapping("/bindings/{bindingId}/pause") public Mono<SyncBindingView> pause(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID bindingId){return service.setBindingEnabled(actor,bindingId,false);}
    @PostMapping("/bindings/{bindingId}/resume") public Mono<SyncBindingView> resume(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID bindingId){return service.setBindingEnabled(actor,bindingId,true);}
    @PostMapping("/conflicts") public Mono<SyncConflictView> conflict(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@Valid @RequestBody CreateSyncConflictRequest request){return service.createConflict(actor,request);}
    @GetMapping("/bindings/{bindingId}/conflicts") public Flux<SyncConflictView> conflicts(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID bindingId){return service.conflicts(actor,bindingId);}
    @PostMapping("/conflicts/{conflictId}/resolve") public Mono<SyncConflictView> resolve(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID conflictId,@RequestParam SyncConflictState state){return service.resolveConflict(actor,conflictId,state);}
    @PostMapping("/devices") public Mono<DeviceView> registerDevice(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@Valid @RequestBody RegisterDeviceRequest request){return service.registerDevice(actor,request);}
    @GetMapping("/devices") public Flux<DeviceView> devices(@RequestHeader("X-Ikaros-Actor-Id") UUID actor){return service.devices(actor);}
    @PostMapping("/devices/{deviceId}/revoke") public Mono<DeviceView> revokeDevice(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID deviceId){return service.revokeDevice(actor,deviceId);}
    @PutMapping("/bindings/{bindingId}/mappings") public Mono<SyncMappingView> upsertMapping(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID bindingId,@Valid @RequestBody UpsertSyncMappingRequest request){return service.upsertMapping(actor,bindingId,request);}
    @GetMapping("/bindings/{bindingId}/mappings") public Flux<SyncMappingView> mappings(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID bindingId){return service.mappings(actor,bindingId);}
    @GetMapping("/spaces/{spaceId}/tombstones") public Flux<DriveTombstoneView> tombstones(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID spaceId,@RequestParam(defaultValue="0") long afterSequence){return service.tombstones(actor,spaceId,afterSequence);}
    @PostMapping("/bindings/{bindingId}/mutations") public Flux<SyncMutationResult> mutations(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID bindingId,@Valid @RequestBody java.util.List<@Valid SyncMutationRequest> requests){return service.applyMutations(actor,bindingId,requests);}
    @PostMapping("/bindings/{bindingId}/cursor") public Mono<SyncBindingView> cursor(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID bindingId,@Valid @RequestBody AdvanceSyncCursorRequest request){return service.advanceCursor(actor,bindingId,request.cursor());}
    @PostMapping("/bindings/{bindingId}/full-resync") public Mono<SyncBindingView> fullResync(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID bindingId){return service.requestFullResync(actor,bindingId);}
    @PutMapping("/bindings/{bindingId}/camera-backups") public Mono<CameraBackupView> cameraBackup(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID bindingId,@Valid @RequestBody CameraBackupRequest request){return service.updateCameraBackup(actor,bindingId,request);}
    @GetMapping("/bindings/{bindingId}/camera-backups") public Flux<CameraBackupView> cameraBackups(@RequestHeader("X-Ikaros-Actor-Id") UUID actor,@PathVariable UUID bindingId){return service.cameraBackups(actor,bindingId);}
    private ResponseEntity<DriveNodeView> withEtag(DriveNodeView view){return ResponseEntity.ok().eTag(IfMatchVersion.etag(view.nodeVersion())).body(view);}
}
