package run.ikaros.server.search.subject;

import static org.apache.commons.lang3.StringUtils.stripToEmpty;
import static org.apache.lucene.document.Field.Store.NO;
import static org.apache.lucene.document.Field.Store.YES;
import static org.apache.lucene.index.IndexWriterConfig.OpenMode.CREATE_OR_APPEND;
import static run.ikaros.api.constant.StringConst.SPACE;
import static run.ikaros.server.infra.utils.TimeUtils.formatTimestamp;

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
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleFragmenter;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.store.AlreadyClosedException;
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
import run.ikaros.api.infra.properties.IkarosProperties;
import run.ikaros.api.search.SearchParam;
import run.ikaros.api.search.SearchResult;
import run.ikaros.api.search.subject.SubjectDoc;
import run.ikaros.api.search.subject.SubjectHint;
import run.ikaros.api.search.subject.SubjectSearchService;
import run.ikaros.api.store.enums.SubjectType;

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
        var subjectIdxPath = ikarosProperties.getWorkDir()
            .resolve("indices").resolve("subjects");
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
    public void updateDocument(List<SubjectDoc> subjectDocs) throws Exception {
        var writeConfig = new IndexWriterConfig(analyzer);
        writeConfig.setOpenMode(CREATE_OR_APPEND);
        try (var writer = new IndexWriter(subjectIndexDir, writeConfig)) {
            subjectDocs.forEach(subjectDoc -> {
                var doc = this.convert(subjectDoc);
                try {
                    var seqNum = writer.updateDocument(new Term(SubjectHint.ID_FIELD,
                            String.valueOf(subjectDoc.getId())),
                        doc);
                    if (log.isDebugEnabled()) {
                        log.debug("Updated document({}) with sequence number {} returned",
                            subjectDoc.getName(), seqNum);
                    }
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            });
        } catch (AlreadyClosedException alreadyClosedException) {
            log.warn("can not rebuild indies for dir has closed.");
        }
    }

    @Override
    public void rebuild(List<SubjectDoc> subjectDocs) throws IOException {
        var writeConfig = new IndexWriterConfig(analyzer);
        writeConfig.setOpenMode(CREATE_OR_APPEND);
        try (var writer = new IndexWriter(subjectIndexDir, writeConfig)) {
            writer.deleteAll();
            subjectDocs.forEach(subjectDoc -> {
                var doc = this.convert(subjectDoc);
                try {
                    var seqNum = writer.updateDocument(new Term(SubjectHint.ID_FIELD,
                            String.valueOf(subjectDoc.getId())),
                        doc);
                    if (log.isDebugEnabled()) {
                        log.debug("Updated document({}) with sequence number {} returned",
                            subjectDoc.getName(), seqNum);
                    }
                } catch (Exception e) {
                    throw Exceptions.propagate(e);
                }
            });
        } catch (AlreadyClosedException alreadyClosedException) {
            log.warn("can not rebuild indies for dir has closed.");
        }
    }

    @Override
    public void removeDocuments(Set<String> termTexts) throws Exception {
        var writeConfig = new IndexWriterConfig(analyzer);
        writeConfig.setOpenMode(CREATE_OR_APPEND);
        try (var writer = new IndexWriter(subjectIndexDir, writeConfig)) {
            var queries = termTexts.stream()
                .map(text -> {
                    try {
                        return buildQuery(text);
                    } catch (ParseException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toArray(Query[]::new);
            long seqNum = writer.deleteDocuments(queries);
            writer.commit();
            log.debug("Deleted documents size [{}] with sequence number {}",
                queries.length, seqNum);
        } catch (AlreadyClosedException alreadyClosedException) {
            log.warn("can not rebuild indies for dir has closed.");
        }
    }

    private Document convert(SubjectDoc subjectDoc) {
        var doc = new Document();
        doc.add(new StringField("id", String.valueOf(subjectDoc.getId()), YES));
        doc.add(new StringField("name", subjectDoc.getName(), YES));
        if (StringUtils.hasText(subjectDoc.getNameCn())) {
            doc.add(new StringField("nameCn", subjectDoc.getNameCn(), YES));
            doc.add(new StringField("namecn", subjectDoc.getNameCn(), YES));
        }
        if (StringUtils.hasText(subjectDoc.getInfobox())) {
            doc.add(new TextField("infobox", subjectDoc.getInfobox(), YES));
        }
        if (StringUtils.hasText(subjectDoc.getSummary())) {
            doc.add(new TextField("summary", subjectDoc.getSummary(), YES));
        }
        if (StringUtils.hasText(subjectDoc.getCover())) {
            doc.add(new TextField("cover", subjectDoc.getCover(), YES));
        }
        doc.add(new StringField("nsfw", String.valueOf(subjectDoc.getNsfw()), YES));
        doc.add(new StringField("type", String.valueOf(subjectDoc.getType()), YES));
        doc.add(new StringField("airTime", formatTimestamp(subjectDoc.getAirTime()), YES));
        doc.add(new StringField("airtime", formatTimestamp(subjectDoc.getAirTime()), YES));
        var content = Jsoup.clean(
            stripToEmpty(String.valueOf(subjectDoc.getId())) + SPACE
                + stripToEmpty(subjectDoc.getName()) + SPACE
                + stripToEmpty(subjectDoc.getNameCn()) + SPACE
                + stripToEmpty(subjectDoc.getInfobox()) + SPACE
                + stripToEmpty(subjectDoc.getSummary()) + SPACE
                + subjectDoc.getNsfw() + SPACE
                + subjectDoc.getType() + SPACE
                + formatTimestamp(subjectDoc.getAirTime()) + SPACE,
            Safelist.none());
        doc.add(new StoredField("content", content));
        doc.add(new TextField("searchable", subjectDoc.getName() + content, NO));
        return doc;
    }


    private SubjectHint convert(Document doc, Highlighter highlighter) {
        return new SubjectHint(
            Long.parseLong(doc.get("id")),
            doc.get("name"), doc.get("nameCn"),
            doc.get("infobox"), doc.get("summary"),
            doc.get("cover"),
            Boolean.valueOf(doc.get("nsfw")),
            SubjectType.valueOf(doc.get("type")),
            doc.get("airTime")
        );
    }

    private Query buildQuery(String keyword) throws ParseException {
        if (keyword.contains(":")) {
            String[] split = keyword.split(":");
            if (split.length == 2) {
                String field = split[0].trim().toLowerCase();
                String value = split[1].trim();
                return new TermQuery(new Term(field, value));
            }
        }
        return buildQuery("searchable", keyword);
    }

    private Query buildQuery(String field, String keyword) throws ParseException {
        if (log.isDebugEnabled()) {
            log.debug("Trying to search for field:keyword: [{}:{}]", field, keyword);
        }
        return new QueryParser(field, analyzer).parse(keyword);
    }
}
