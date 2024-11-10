package run.ikaros.api.search.subject;

import java.io.IOException;
import java.util.List;
import run.ikaros.api.search.IndicesSearchService;
import run.ikaros.api.search.SearchParam;
import run.ikaros.api.search.SearchResult;

public interface SubjectSearchService extends IndicesSearchService {

    SearchResult<SubjectHint> search(SearchParam param) throws Exception;

    void updateDocument(List<SubjectDoc> subjectDocs) throws Exception;

    void rebuild(List<SubjectDoc> subjectDocs) throws IOException;
}
