package run.ikaros.server.crd;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * @author: li-guohao
 */
@Data
@EqualsAndHashCode
public class Metadata {
    /**
     * 全局唯一
     */
    private String name;
    private String generateName;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    private Long version;
    private Instant creationTimestamp;
    private Instant deletionTimestamp;
    private Set<String> finalizers;
}
