package run.ikaros.server.crd;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.springframework.util.StringUtils;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * @author: li-guohao
 * @see CustomResourceDefinition
 */
public interface CRDOperator {

    @Nullable
    @JsonProperty("apiVersion")
    @Schema(requiredMode = REQUIRED)
    default String getApiVersion() {
        final var gvk = getClass().getAnnotation(GVK.class);
        if (gvk == null) {
            return null;
        }
        if (StringUtils.hasText(gvk.group())) {
            return gvk.group() + "/" + gvk.version();
        }
        return gvk.version();
    }


    @Nullable
    @JsonProperty("kind")
    @Schema(requiredMode = REQUIRED)
    default String getKind() {
        final var gvk = getClass().getAnnotation(GVK.class);
        if (gvk == null) {
            return null;
        }
        return gvk.kind();
    }

    void setApiVersion(@Nonnull String apiVersion);

    void setKind(@Nonnull String kind);


    @Nonnull
    @Schema(implementation = Metadata.class, requiredMode = REQUIRED)
    @JsonProperty("metadata")
    Metadata getMetadata();

    void setMetadata(@Nonnull Metadata metadata);

    default void groupVersionKind(GroupVersionKind gvk) {
        setApiVersion(gvk.groupVersion().toString());
        setKind(gvk.kind());
    }

    @JsonIgnore
    default GroupVersionKind groupVersionKind() {
        return null;
    }
}
