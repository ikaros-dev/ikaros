package run.ikaros.api.search.file;

import java.io.IOException;
import java.util.List;
import run.ikaros.api.search.IndicesSearchService;
import run.ikaros.api.search.SearchParam;
import run.ikaros.api.search.SearchResult;


public interface FileSearchService extends IndicesSearchService {

    SearchResult<FileHint> search(SearchParam searchParam) throws Exception;

    void updateDocument(List<FileDoc> fileDocs) throws Exception;

    void rebuild(List<FileDoc> fileDocs) throws IOException;

}
