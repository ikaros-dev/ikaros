package run.ikaros.storage;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** 登记派生附件时接受的来源附件与新内容信息。 */
public record CreateDerivedAttachmentRequest(@NotNull UUID sourceAttachmentId, @NotNull @Valid AttachBlobRequest content) { }
