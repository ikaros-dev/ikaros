package run.ikaros.api.search.file;

import java.util.List;
import run.ikaros.api.search.IndicesSearchService;
import run.ikaros.api.search.SearchParam;
import run.ikaros.api.search.SearchResult;


public interface FileSearchService extends IndicesSearchService {

    SearchResult<FileHint> search(SearchParam searchParam) throws Exception;

    void addDocuments(List<FileDoc> fileDocs) throws Exception;

}
