package run.ikaros.server.crd.scheme;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.lang.NonNull;
import run.ikaros.server.crd.CustomResourceDefinition;
import run.ikaros.server.crd.GVK;
import run.ikaros.server.crd.GroupVersionKind;
import run.ikaros.server.crd.exception.CRDException;

/**
 * custom resource definition scheme
 *
 * @author: li-guohao
 * @see CustomResourceDefinition
 */
public record CRDScheme(Class<? extends CustomResourceDefinition> type,
                        GroupVersionKind groupVersionKind,
                        String plural,
                        String singular,
                        ObjectNode openApiSchema) {


    @NonNull
    public static GVK getGvkFromType(@NonNull Class<? extends CustomResourceDefinition> type) {
        var gvk = type.getAnnotation(GVK.class);
        if (gvk == null) {
            throw new CRDException(
                String.format("Annotation %s needs to be on Extension %s", GVK.class.getName(),
                    type.getName()));
        }
        return gvk;
    }
}
