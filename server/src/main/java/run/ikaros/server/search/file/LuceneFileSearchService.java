package run.ikaros.server.search.file;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;
import static org.apache.lucene.index.IndexWriterConfig.OpenMode.APPEND;
import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE_OR_APPEND;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import org.wltea.analyzer.lucene.IKAnalyzer;
import reactor.core.Exceptions;
import run.ikaros.api.search.SearchParam;
import run.ikaros.api.search.SearchResult;
import run.ikaros.api.search.file.FileDoc;
import run.ikaros.api.search.file.FileHint;
import run.ikaros.api.search.file.FileSearchService;
import run.ikaros.server.infra.properties.IkarosProperties;

@Slf4j
@Service
public class LuceneFileSearchService implements FileSearchService, DisposableBean {
    public static final int MAX_FRAGMENT_SIZE = 100;

    private final Analyzer analyzer;

    private final Directory fileIndexDir;

    /**
     * Construct.
     */
    public LuceneFileSearchService(IkarosProperties ikarosProperties) throws IOException {
        analyzer = new IKAnalyzer(true);
        var postIdxPath = ikarosProperties.getWorkDir().resolve("indices/files");
        fileIndexDir = FSDirectory.open(postIdxPath);
    }


    @Override
    public void destroy() throws Exception {
        analyzer.close();
        fileIndexDir.close();
    }

    @Override
    public void removeDocuments(Set<String> names) throws Exception {
        var writeConfig = new IndexWriterConfig(analyzer);
        writeConfig.setOpenMode(APPEND);
        try (var writer = new IndexWriter(fileIndexDir, writeConfig)) {
            var terms = names.stream()
                .map(name -> new Term(FileHint.ID_FIELD, name))
                .toArray(Term[]::new);
            long seqNum = writer.deleteDocuments(terms);
            log.debug("Deleted documents({}) with sequence number {}", terms.length, seqNum);
        }
    }

    @Override
    public SearchResult<FileHint> search(SearchParam param) throws Exception {
        var dirReader = DirectoryReader.open(fileIndexDir);
        var searcher = new IndexSearcher(dirReader);
        var keyword = param.getKeyword();
        var watch = new StopWatch("SearchWatch");
        watch.start("search for " + keyword);
        var query = buildQuery(keyword);
        var topDocs = searcher.search(query, param.getLimit(), Sort.RELEVANCE);
        watch.stop();
        var highlighter = new Highlighter(
            new SimpleHTMLFormatter(param.getHighlightPreTag(), param.getHighlightPostTag()),
            new QueryScorer(query));
        highlighter.setTextFragmenter(new SimpleFragmenter(MAX_FRAGMENT_SIZE));

        var hits = new ArrayList<FileHint>(topDocs.scoreDocs.length);
        for (var scoreDoc : topDocs.scoreDocs) {
            hits.add(convert(searcher.storedFields().document(scoreDoc.doc), highlighter));
        }

        var result = new SearchResult<FileHint>();
        result.setHits(hits);
        result.setTotal(topDocs.totalHits.value);
        result.setKeyword(param.getKeyword());
        result.setLimit(param.getLimit());
        result.setProcessingTimeMillis(watch.getTotalTimeMillis());
        return result;
    }

    @Override
    public void addDocuments(List<FileDoc> fileDocs) throws Exception {
        var writeConfig = new IndexWriterConfig(analyzer);
        writeConfig.setOpenMode(CREATE_OR_APPEND);
        try (var writer = new IndexWriter(fileIndexDir, writeConfig)) {
            fileDocs.forEach(fileDoc -> {
                var doc = this.convert(fileDoc);
                try {
                    var seqNum =
                        writer.updateDocument(new Term(FileHint.ID_FIELD, fileDoc.getName()), doc);
                    if (log.isDebugEnabled()) {
                        log.debug("Updated document({}) with sequence number {} returned",
                            fileDoc.getName(), seqNum);
                    }
                } catch (IOException e) {
                    throw Exceptions.propagate(e);
                }
            });
        }
    }

    private Document convert(FileDoc fileDoc) {
        var doc = new Document();
        doc.add(new StringField("name", fileDoc.getName(), YES));
        doc.add(new StringField("url", fileDoc.getUrl(), YES));
        doc.add(new StringField("type", String.valueOf(fileDoc.getType()), YES));
        doc.add(new StringField("place", String.valueOf(fileDoc.getPlace()), YES));
        var content = Jsoup.clean(stripToEmpty(fileDoc.getName())
                + stripToEmpty(fileDoc.getUrl())
                + fileDoc.getType()
                + fileDoc.getPlace(),
            Safelist.none());
        doc.add(new StoredField("content", content));
        doc.add(new TextField("searchable", fileDoc.getName() + content, NO));
        return doc;
    }


    private FileHint convert(Document doc, Highlighter highlighter)
        throws IOException, InvalidTokenOffsetsException {
        var post = new FileHint(
            doc.get("name"),
            doc.get("originalPath"),
            doc.get("url"),
            null,
            null
        );
        return post;
    }

    private Query buildQuery(String keyword) throws ParseException {
        if (log.isDebugEnabled()) {
            log.debug("Trying to search for keyword: {}", keyword);
        }
        return new QueryParser("searchable", analyzer).parse(keyword);
    }

}
