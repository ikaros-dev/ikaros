package run.ikaros.server.search.subject;

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
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;
import org.wltea.analyzer.lucene.IKAnalyzer;
import reactor.core.Exceptions;
import run.ikaros.api.search.SearchParam;
import run.ikaros.api.search.SearchResult;
import run.ikaros.api.search.file.FileHint;
import run.ikaros.api.search.subject.SubjectDoc;
import run.ikaros.api.search.subject.SubjectHint;
import run.ikaros.api.search.subject.SubjectSearchService;
import run.ikaros.api.store.enums.SubjectType;
import run.ikaros.server.infra.properties.IkarosProperties;

@Slf4j
@Component
public class LuceneSubjectSearchService implements SubjectSearchService, DisposableBean {
    public static final int MAX_FRAGMENT_SIZE = 100;

    private final Analyzer analyzer;

    private final Directory subjectIndexDir;

    /**
     * Construct.
     */
    public LuceneSubjectSearchService(IkarosProperties ikarosProperties) throws IOException {
        analyzer = new IKAnalyzer(true);
        var subjectIdxPath = ikarosProperties.getWorkDir().resolve("indices/subjects");
        subjectIndexDir = FSDirectory.open(subjectIdxPath);
    }

    @Override
    public void destroy() throws Exception {
        analyzer.close();
        subjectIndexDir.close();
    }

    @Override
    public SearchResult<SubjectHint> search(SearchParam param) throws Exception {
        var dirReader = DirectoryReader.open(subjectIndexDir);
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

        var hits = new ArrayList<SubjectHint>(topDocs.scoreDocs.length);
        for (var scoreDoc : topDocs.scoreDocs) {
            hits.add(convert(searcher.storedFields().document(scoreDoc.doc), highlighter));
        }

        var result = new SearchResult<SubjectHint>();
        result.setHits(hits);
        result.setTotal(topDocs.totalHits.value);
        result.setKeyword(param.getKeyword());
        result.setLimit(param.getLimit());
        result.setProcessingTimeMillis(watch.getTotalTimeMillis());
        return result;
    }

    @Override
    public void addDocuments(List<SubjectDoc> subjectDocs) throws Exception {
        var writeConfig = new IndexWriterConfig(analyzer);
        writeConfig.setOpenMode(CREATE_OR_APPEND);
        try (var writer = new IndexWriter(subjectIndexDir, writeConfig)) {
            subjectDocs.forEach(subjectDoc -> {
                var doc = this.convert(subjectDoc);
                try {
                    var seqNum =
                        writer.updateDocument(new Term(SubjectHint.ID_FIELD, subjectDoc.getName()),
                            doc);
                    if (log.isDebugEnabled()) {
                        log.debug("Updated document({}) with sequence number {} returned",
                            subjectDoc.getName(), seqNum);
                    }
                } catch (IOException e) {
                    throw Exceptions.propagate(e);
                }
            });
        }
    }

    @Override
    public void removeDocuments(Set<String> names) throws Exception {
        var writeConfig = new IndexWriterConfig(analyzer);
        writeConfig.setOpenMode(APPEND);
        try (var writer = new IndexWriter(subjectIndexDir, writeConfig)) {
            var terms = names.stream()
                .map(name -> new Term(FileHint.ID_FIELD, name))
                .toArray(Term[]::new);
            long seqNum = writer.deleteDocuments(terms);
            log.debug("Deleted documents({}) with sequence number {}", terms.length, seqNum);
        }
    }

    private Document convert(SubjectDoc subjectDoc) {
        var doc = new Document();
        doc.add(new StringField("name", subjectDoc.getName(), YES));
        if (StringUtils.hasText(subjectDoc.getNameCn())) {
            doc.add(new StringField("nameCn", subjectDoc.getNameCn(), YES));
        }
        if (StringUtils.hasText(subjectDoc.getInfobox())) {
            doc.add(new StringField("infobox", subjectDoc.getInfobox(), YES));
        }
        if (StringUtils.hasText(subjectDoc.getSummary())) {
            doc.add(new StringField("summary", subjectDoc.getSummary(), YES));
        }
        doc.add(new StoredField("nsfw", String.valueOf(subjectDoc.getNsfw())));
        doc.add(new StoredField("type", String.valueOf(subjectDoc.getType())));
        doc.add(new StoredField("airTime", subjectDoc.getAirTime()));
        var content = Jsoup.clean(stripToEmpty(subjectDoc.getName())
                + stripToEmpty(subjectDoc.getNameCn())
                + stripToEmpty(subjectDoc.getInfobox())
                + stripToEmpty(subjectDoc.getSummary())
                + subjectDoc.getNsfw()
                + subjectDoc.getNsfw()
                + subjectDoc.getType()
                + subjectDoc.getAirTime(),
            Safelist.none());
        doc.add(new StoredField("content", content));
        doc.add(new TextField("searchable", subjectDoc.getName() + content, NO));
        return doc;
    }


    private SubjectHint convert(Document doc, Highlighter highlighter) {
        return new SubjectHint(
            doc.get("name"), doc.get("nameCn"),
            doc.get("infobox"), doc.get("summary"),
            Boolean.valueOf(doc.get("nsfw")),
            SubjectType.valueOf(doc.get("type")),
            Long.parseLong(doc.get("airTime"))
        );
    }

    private Query buildQuery(String keyword) throws ParseException {
        if (log.isDebugEnabled()) {
            log.debug("Trying to search for keyword: {}", keyword);
        }
        return new QueryParser("searchable", analyzer).parse(keyword);
    }
}
