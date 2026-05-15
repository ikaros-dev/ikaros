package run.ikaros.api.search;

import java.util.Set;
import org.pf4j.ExtensionPoint;

public interface IndicesSearchService extends ExtensionPoint {

    void removeDocuments(Set<String> termTexts) throws Exception;
}
