package run.ikaros.server.crd;

import java.util.Comparator;
import java.util.Objects;

/**
 * CRD: Custom Resource Definition
 *
 * @author: li-guohao
 */
public interface CustomResourceDefinition
    extends CRDOperator, Comparable<CustomResourceDefinition> {
    @Override
    default int compareTo(CustomResourceDefinition another) {
        if (another == null || another.getMetadata() == null) {
            return 1;
        }
        if (getMetadata() == null) {
            return -1;
        }
        return Objects.compare(getMetadata().getName(), another.getMetadata().getName(),
            Comparator.naturalOrder());
    }
}
