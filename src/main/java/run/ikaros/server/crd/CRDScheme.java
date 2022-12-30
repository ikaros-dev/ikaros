package run.ikaros.server.crd;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.lang.NonNull;

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
