package run.ikaros.api.search;

import java.util.Set;
import run.ikaros.api.plugin.IkarosExtensionPoint;

public interface IndicesSearchService extends IkarosExtensionPoint {

    void removeDocuments(Set<String> termTexts) throws Exception;
}
